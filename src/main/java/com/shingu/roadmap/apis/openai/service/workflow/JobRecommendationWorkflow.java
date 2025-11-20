package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.apis.saramin.client.SaraminClient;
import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.diagnosis.dto.response.JobRecommendationResponse;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 채용공고 추천 워크플로우 (실전 버전)
 *
 * 개선 사항:
 * 1. NCS 코드만으로 직접 Saramin API 검색 (희망 직무 의존성 제거)
 * 2. AI 평가 간소화 (비용 절감)
 * 3. Saramin API 실패 시 graceful degradation
 * 4. 상세한 로깅 및 에러 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecommendationWorkflow {

    private final SaraminClient saraminClient;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final NcsApiService ncsApiService;
    private final ResumeTextFormatter resumeTextFormatter;

    /**
     * 사용자 프로필과 NCS 코드를 기반으로 채용공고를 추천합니다.
     *
     * 전략:
     * 1. 사용자의 희망 직무가 있으면 우선 사용
     * 2. 없으면 NCS 직무명 키워드로 검색
     * 3. 검색 결과가 없으면 IT 일반 직무로 폴백
     *
     * @param profile 사용자 프로필
     * @param ncsCode 추천된 NCS 코드
     * @return 추천 채용공고 리스트
     */
    @Cacheable(value = OpenAiCacheConfig.JOB_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<List<JobRecommendationResponse>> recommendJobs(Profile profile, String ncsCode) {
        log.info("[JobRecommendationWorkflow] Starting job recommendation - memberId: {}, ncsCode: {}",
                profile.getMember().getId(), ncsCode);

        // 1. NCS 코드 검증 및 직무 정보 조회
        return Mono.fromCallable(() -> {
            Set<NcsOccupation> validOccupations = ncsApiService.filterValidNcsCodes(Set.of(ncsCode));
            if (validOccupations.isEmpty()) {
                log.warn("[JobRecommendationWorkflow] Invalid NCS code: {}, returning empty list", ncsCode);
                return null; // null을 반환하여 다음 단계에서 처리
            }
            return validOccupations.iterator().next();
        }).flatMap(ncsOccupation -> {
            if (ncsOccupation == null) {
                log.warn("[JobRecommendationWorkflow] NCS occupation is null, returning empty list");
                return Mono.just(Collections.<JobRecommendationResponse>emptyList());
            }

            log.info("[JobRecommendationWorkflow] NCS occupation found - code: {}, name: {}",
                    ncsOccupation.getDutyCd(), ncsOccupation.getDutyNm());

            // 2. Saramin API 호출 전략 결정
            return fetchJobsFromSaramin(profile, ncsOccupation)
                    .flatMap(jobs -> {
                        if (jobs.isEmpty()) {
                            log.warn("[JobRecommendationWorkflow] No jobs found, returning empty list");
                            return Mono.just(Collections.<JobRecommendationResponse>emptyList());
                        }

                        log.info("[JobRecommendationWorkflow] Found {} jobs from Saramin API", jobs.size());

                        // 3. 채용공고를 간단히 변환 (AI 평가는 선택적)
                        return convertToRecommendations(profile, ncsOccupation, jobs);
                    });
        })
        .doOnSuccess(recommendations ->
                log.info("[JobRecommendationWorkflow] Job recommendation completed - count: {}",
                        recommendations != null ? recommendations.size() : 0)
        )
        .doOnError(e ->
                log.error("[JobRecommendationWorkflow] Error during job recommendation: {}", e.getMessage(), e)
        )
        .onErrorResume(e -> {
            log.error("[JobRecommendationWorkflow] Returning empty list due to error", e);
            return Mono.just(Collections.emptyList());
        });
    }

    /**
     * Saramin API에서 채용공고를 가져옵니다.
     *
     * 우선순위:
     * 1. 사용자 희망 직무 코드
     * 2. NCS 직무명 키워드 검색
     * 3. 폴백: IT 전체 직군
     */
    private Mono<List<SaraminJobListResponse.Jobs.Job>> fetchJobsFromSaramin(
            Profile profile,
            NcsOccupation ncsOccupation) {

        return Mono.fromCallable(() -> {
            SaraminJobListResponse response = null;

            // 전략 1: 사용자 희망 직무 코드 사용
            Set<Integer> desiredJobCodes = profile.getDesiredJobs() != null ?
                    profile.getDesiredJobs().stream()
                            .map(SaraminJob::getCode)
                            .collect(Collectors.toSet()) : Collections.emptySet();

            if (!desiredJobCodes.isEmpty()) {
                log.info("[JobRecommendationWorkflow] Using desired job codes: {}", desiredJobCodes);
                try {
                    response = saraminClient.getJobList(null, 0, null, null, desiredJobCodes, null);
                    if (response != null && response.jobs() != null &&
                        response.jobs().job() != null && !response.jobs().job().isEmpty()) {
                        log.info("[JobRecommendationWorkflow] Found {} jobs with desired job codes",
                                response.jobs().job().size());
                        return response.jobs().job();
                    }
                } catch (Exception e) {
                    log.warn("[JobRecommendationWorkflow] Failed to fetch with desired job codes: {}", e.getMessage());
                }
            }

            // 전략 2: NCS 직무명으로 키워드 검색
            String ncsJobName = ncsOccupation.getDutyNm();
            if (ncsJobName != null && !ncsJobName.isEmpty()) {
                log.info("[JobRecommendationWorkflow] Using NCS job name keyword: {}", ncsJobName);
                try {
                    // Saramin API는 키워드 검색을 지원하지만, 현재 클라이언트 구현에서는 제한적
                    // 대신 직무 그룹 코드로 검색 (IT 개발: 2, 디자인: 3 등)
                    Set<Integer> itJobGroupCodes = Set.of(2); // IT/인터넷 그룹
                    response = saraminClient.getJobList(null, 0, null, itJobGroupCodes, null, null);

                    if (response != null && response.jobs() != null &&
                        response.jobs().job() != null && !response.jobs().job().isEmpty()) {
                        log.info("[JobRecommendationWorkflow] Found {} jobs with IT job group",
                                response.jobs().job().size());
                        return response.jobs().job();
                    }
                } catch (Exception e) {
                    log.warn("[JobRecommendationWorkflow] Failed to fetch with NCS keyword: {}", e.getMessage());
                }
            }

            // 전략 3: 폴백 - IT 전체 최신 공고
            log.info("[JobRecommendationWorkflow] Fallback: fetching latest IT jobs");
            try {
                Set<Integer> fallbackJobCodes = Set.of(84, 91, 92); // 백엔드, 프론트엔드, 풀스택
                response = saraminClient.getJobList(null, 0, null, null, fallbackJobCodes, null);

                if (response != null && response.jobs() != null &&
                    response.jobs().job() != null && !response.jobs().job().isEmpty()) {
                    log.info("[JobRecommendationWorkflow] Found {} jobs with fallback strategy",
                            response.jobs().job().size());
                    return response.jobs().job();
                }
            } catch (Exception e) {
                log.error("[JobRecommendationWorkflow] Fallback strategy also failed: {}", e.getMessage());
            }

            log.warn("[JobRecommendationWorkflow] All strategies failed, returning empty list");
            return Collections.<SaraminJobListResponse.Jobs.Job>emptyList();
        });
    }

    /**
     * Saramin 채용공고를 추천 응답 DTO로 변환합니다.
     *
     * 간소화 버전: AI 평가 없이 직접 변환
     * - 비용 절감
     * - 응답 속도 향상
     * - Saramin API 데이터 직접 활용
     */
    private Mono<List<JobRecommendationResponse>> convertToRecommendations(
            Profile profile,
            NcsOccupation ncsOccupation,
            List<SaraminJobListResponse.Jobs.Job> jobs) {

        log.info("[JobRecommendationWorkflow] Converting {} jobs to recommendations (simple mode)", jobs.size());

        List<JobRecommendationResponse> recommendations = jobs.stream()
                .limit(10) // 최대 10개
                .map(job -> {
                    // 간단한 매칭 점수 계산 (실제로는 더 정교한 로직 가능)
                    int matchScore = calculateSimpleMatchScore(profile, job);
                    String reason = generateSimpleReason(ncsOccupation, job);

                    return JobRecommendationResponse.builder()
                            .jobId(job.id())
                            .title(job.position().title())
                            .companyName(job.company().detail().name())
                            .companyLogoUrl(job.company().detail().logoUrl())
                            .url(job.url())
                            .location(job.position().location() != null ?
                                    job.position().location().name() : null)
                            .experienceLevel(job.position().experienceLevel() != null ?
                                    job.position().experienceLevel().name() : null)
                            .educationLevel(job.position().requiredEducationLevel() != null ?
                                    job.position().requiredEducationLevel().name() : null)
                            .jobCode(job.position().jobCode() != null ?
                                    job.position().jobCode().code() : null)
                            .jobName(job.position().jobCode() != null ?
                                    job.position().jobCode().name() : null)
                            .salary(job.salary() != null ? job.salary().name() : null)
                            .expirationTimestamp(job.expirationTimestamp())
                            .recommendationReason(reason)
                            .matchScore(matchScore)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("[JobRecommendationWorkflow] Converted {} recommendations", recommendations.size());
        return Mono.just(recommendations);
    }

    /**
     * 간단한 매칭 점수 계산
     *
     * 기준:
     * - 직무 제목에 사용자 스킬 키워드 포함 여부
     * - 경력 요구사항 일치도
     * - 학력 요구사항 일치도
     */
    private int calculateSimpleMatchScore(Profile profile, SaraminJobListResponse.Jobs.Job job) {
        int score = 50; // 기본 점수

        String jobTitle = job.position().title().toLowerCase();
        String keyword = job.keyword() != null ? job.keyword().toLowerCase() : "";

        // 사용자 스킬과 매칭
        if (profile.getProfileSkills() != null) {
            long matchedSkills = profile.getProfileSkills().stream()
                    .map(ps -> ps.getSkill().getName().toLowerCase())
                    .filter(skillName -> jobTitle.contains(skillName) || keyword.contains(skillName))
                    .count();

            score += Math.min(matchedSkills * 10, 30); // 최대 30점 추가
        }

        // 경력 레벨 매칭 (신입 우대)
        if (job.position().experienceLevel() != null) {
            int minExp = job.position().experienceLevel().min();
            if (minExp == 0) {
                score += 10; // 신입 가능하면 10점 추가
            }
        }

        // 학력 요구사항 확인 (제한 없으면 점수 추가)
        if (job.position().requiredEducationLevel() == null ||
            "학력무관".equals(job.position().requiredEducationLevel().name())) {
            score += 10;
        }

        return Math.min(score, 100); // 최대 100점
    }

    /**
     * 간단한 추천 이유 생성
     */
    private String generateSimpleReason(NcsOccupation ncsOccupation, SaraminJobListResponse.Jobs.Job job) {
        String ncsName = ncsOccupation.getDutyNm();
        String companyName = job.company().detail().name();
        String jobName = job.position().jobCode() != null ?
                job.position().jobCode().name() : "해당 직무";

        return String.format("%s 직무와 관련된 %s 포지션입니다. %s에서 모집 중입니다.",
                ncsName, jobName, companyName);
    }

    /**
     * AI 평가 결과 DTO (향후 활성화 시 사용)
     */
    @SuppressWarnings("unused")
    private record JobEvaluationDto(
            String jobId,
            Integer matchScore,
            String recommendationReason
    ) {}
}

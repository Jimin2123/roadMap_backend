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
import com.shingu.roadmap.apis.saramin.domain.SaraminRegion;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.apis.saramin.repository.SaraminRegionRepository;
import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.diagnosis.dto.response.JobRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.resume.domain.Career;
import com.shingu.roadmap.resume.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 기반 채용공고 추천 워크플로우 (OpenAI 완전 활용 버전)
 *
 * 핵심 개선 사항:
 * 1. 규칙 기반 매칭 제거 → OpenAI가 모든 평가 수행
 * 2. 페이지네이션을 통한 적합도 높은 공고 수집
 * 3. KSA 분석 결과를 포함한 종합적 프로필 평가
 * 4. 최소 매칭 점수(65점) 이상 공고만 필터링
 * 5. AI가 생성한 맞춤형 추천 이유 제공
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
    private final SaraminRegionRepository saraminRegionRepository;

    /**
     * 채용공고 추천 설정 상수
     */
    private static final int MIN_MATCH_SCORE = 65;  // 최소 매칭 점수
    private static final int TARGET_JOB_COUNT = 10; // 목표 추천 공고 개수
    private static final int MAX_PAGES = 5;          // 최대 탐색 페이지 수
    private static final int JOBS_PER_PAGE = 20;    // 페이지당 채용공고 수

    /**
     * 사용자 프로필, KSA 분석 결과, NCS 코드를 기반으로 AI가 채용공고를 추천합니다.
     *
     * 페이지네이션 전략:
     * 1. 1페이지부터 시작하여 Saramin API에서 채용공고 조회
     * 2. OpenAI를 사용하여 각 공고의 매칭 점수와 추천 이유 생성
     * 3. 매칭 점수 65점 이상인 공고만 수집
     * 4. 10개 이상 수집될 때까지 다음 페이지로 이동 (최대 5페이지)
     * 5. 수집된 공고를 매칭 점수 순으로 정렬하여 반환
     *
     * @param profile 사용자 프로필 (경력, 학력, 스킬, 자격증 포함)
     * @param ncsCode 추천된 NCS 코드
     * @param ksaAnalysis KSA 역량 분석 결과 (선택적)
     * @return 추천 채용공고 리스트
     */
    @Cacheable(value = OpenAiCacheConfig.JOB_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<List<JobRecommendationResponse>> recommendJobs(
            Profile profile,
            String ncsCode,
            KsaAnalysisResponse ksaAnalysis) {

        log.info("[JobRecommendationWorkflow] Starting AI-based job recommendation - memberId: {}, ncsCode: {}",
                profile.getMember().getId(), ncsCode);

        // 1. NCS 코드 검증 및 직무 정보 조회
        return Mono.fromCallable(() -> {
            Set<NcsOccupation> validOccupations = ncsApiService.filterValidNcsCodes(Set.of(ncsCode));
            if (validOccupations.isEmpty()) {
                log.warn("[JobRecommendationWorkflow] Invalid NCS code: {}, returning empty list", ncsCode);
                return null;
            }
            return validOccupations.iterator().next();
        }).flatMap(ncsOccupation -> {
            if (ncsOccupation == null) {
                log.warn("[JobRecommendationWorkflow] NCS occupation is null, returning empty list");
                return Mono.just(Collections.<JobRecommendationResponse>emptyList());
            }

            log.info("[JobRecommendationWorkflow] NCS occupation found - code: {}, name: {}",
                    ncsOccupation.getDutyCd(), ncsOccupation.getDutyNm());

            // 2. 페이지네이션을 통한 적합 공고 수집
            return collectQualifiedJobsWithPagination(profile, ncsOccupation, ksaAnalysis);
        })
        .doOnSuccess(recommendations ->
                log.info("[JobRecommendationWorkflow] AI job recommendation completed - count: {}",
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
     * 하위 호환성을 위한 오버로드 메서드 (KSA 분석 없이 호출 가능)
     */
    @Cacheable(value = OpenAiCacheConfig.JOB_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<List<JobRecommendationResponse>> recommendJobs(Profile profile, String ncsCode) {
        return recommendJobs(profile, ncsCode, null);
    }

    /**
     * 페이지네이션을 통해 적합한 채용공고를 수집합니다.
     *
     * 로직:
     * 1. 1페이지부터 순차적으로 Saramin API 호출
     * 2. 각 페이지의 공고를 OpenAI로 평가
     * 3. 매칭 점수 65점 이상만 수집
     * 4. 10개 수집되거나 최대 페이지에 도달할 때까지 반복
     */
    private Mono<List<JobRecommendationResponse>> collectQualifiedJobsWithPagination(
            Profile profile,
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis) {

        return Mono.fromCallable(() -> {
            List<JobRecommendationResponse> qualifiedJobs = new ArrayList<>();
            int currentPage = 0; // Saramin API는 0부터 시작
            int pagesExplored = 0;

            log.info("[JobRecommendationWorkflow] Starting pagination - target: {} jobs, maxPages: {}",
                    TARGET_JOB_COUNT, MAX_PAGES);

            // LazyInitializationException 방지: 지역 정보를 미리 추출 (JPA 세션 내에서)
            final String userLocation = extractUserLocation(profile);
            log.info("[JobRecommendationWorkflow] User location extracted: {}", userLocation != null ? userLocation : "not specified");

            // 검색 전략 결정 (사용자 희망 직무 우선)
            Set<Integer> desiredJobCodes = extractDesiredJobCodes(profile);
            Set<Integer> jobGroupCodes = desiredJobCodes.isEmpty() ? Set.of(2) : null; // IT 그룹 폴백

            while (qualifiedJobs.size() < TARGET_JOB_COUNT && pagesExplored < MAX_PAGES) {
                pagesExplored++;
                log.info("[JobRecommendationWorkflow] Fetching page {} (start position: {})",
                        pagesExplored, currentPage);

                try {
                    // Saramin API 호출
                    SaraminJobListResponse response = saraminClient.getJobList(
                            null, currentPage, null, jobGroupCodes, desiredJobCodes, null);

                    if (response == null || response.jobs() == null || response.jobs().job() == null) {
                        log.warn("[JobRecommendationWorkflow] No response from Saramin API on page {}", pagesExplored);
                        break;
                    }

                    List<SaraminJobListResponse.Jobs.Job> jobs = response.jobs().job();
                    int totalJobs = Integer.parseInt(response.jobs().total());

                    log.info("[JobRecommendationWorkflow] Page {} fetched - jobs: {}, total available: {}",
                            pagesExplored, jobs.size(), totalJobs);

                    if (jobs.isEmpty()) {
                        log.info("[JobRecommendationWorkflow] No more jobs available, stopping pagination");
                        break;
                    }

                    // OpenAI를 사용하여 각 공고 평가
                    List<JobRecommendationResponse> evaluatedJobs = evaluateJobsWithAI(
                            profile, ncsOccupation, ksaAnalysis, jobs, userLocation);

                    // 매칭 점수 65점 이상만 필터링
                    List<JobRecommendationResponse> qualified = evaluatedJobs.stream()
                            .filter(job -> job.matchScore() != null && job.matchScore() >= MIN_MATCH_SCORE)
                            .toList();

                    qualifiedJobs.addAll(qualified);

                    log.info("[JobRecommendationWorkflow] Page {} evaluation complete - qualified: {}/{}, total collected: {}",
                            pagesExplored, qualified.size(), jobs.size(), qualifiedJobs.size());

                    // 다음 페이지로 이동
                    currentPage += JOBS_PER_PAGE;

                    // 더 이상 페이지가 없으면 종료
                    if (currentPage >= totalJobs) {
                        log.info("[JobRecommendationWorkflow] Reached end of available jobs");
                        break;
                    }

                } catch (Exception e) {
                    log.error("[JobRecommendationWorkflow] Error fetching page {}: {}",
                            pagesExplored, e.getMessage(), e);
                    break;
                }
            }

            log.info("[JobRecommendationWorkflow] Pagination complete - collected {} qualified jobs from {} pages",
                    qualifiedJobs.size(), pagesExplored);

            // 매칭 점수 순으로 정렬하여 상위 10개 반환
            return qualifiedJobs.stream()
                    .sorted(Comparator.comparingInt(JobRecommendationResponse::matchScore).reversed())
                    .limit(TARGET_JOB_COUNT)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 사용자 프로필에서 희망 직무 코드 추출
     */
    private Set<Integer> extractDesiredJobCodes(Profile profile) {
        if (profile.getDesiredJobs() == null || profile.getDesiredJobs().isEmpty()) {
            return Collections.emptySet();
        }
        return profile.getDesiredJobs().stream()
                .map(SaraminJob::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자의 희망 근무 지역을 추출합니다.
     *
     * LazyInitializationException 방지를 위해 JPA 세션 내에서 호출되어야 합니다.
     * 이 메서드는 collectQualifiedJobsWithPagination() 시작 시점에 호출되어
     * 지역 정보를 미리 추출하고 파라미터로 전달하는 방식으로 사용됩니다.
     *
     * @param profile 사용자 프로필
     * @return 사용자 지역명 (예: "서울특별시", "경기도", null)
     */
    private String extractUserLocation(Profile profile) {
        try {
            if (profile.getMember() != null && profile.getMember().getAddress() != null) {
                return profile.getMember().getAddress().getRegionCity();
            }
        } catch (Exception e) {
            log.warn("[JobRecommendationWorkflow] Failed to extract user location: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 사용자의 희망 최소 급여를 추출합니다.
     *
     * Resume → DesiredCompany → desiredSalary에서 가져옵니다.
     * Profile.desiredMinSalary 필드는 중복이므로 제거되었습니다.
     *
     * @param profile 사용자 프로필
     * @return 희망 최소 급여 (만원 단위, NULL이면 조건 없음)
     */
    private Integer extractDesiredMinSalary(Profile profile) {
        try {
            if (profile.getResume() != null && profile.getResume().getDesiredCompany() != null) {
                int desiredSalary = profile.getResume().getDesiredCompany().getDesiredSalary();
                // desiredSalary가 0이면 희망 급여 없음으로 간주
                return desiredSalary > 0 ? desiredSalary : null;
            }
        } catch (Exception e) {
            log.warn("[JobRecommendationWorkflow] Failed to extract desired salary: {}", e.getMessage());
        }
        return null;
    }

    /**
     * OpenAI를 사용하여 채용공고 목록을 평가합니다.
     *
     * AI가 수행하는 평가:
     * 1. 사용자 프로필 (경력, 학력, 스킬, 자격증) 분석
     * 2. KSA 역량 분석 결과 고려
     * 3. 각 채용공고의 요구사항과 사용자 역량 비교
     * 4. 0-100점 매칭 점수 산출
     * 5. 맞춤형 추천 이유 생성
     *
     * 사전 필터링 제거: OpenAI가 모든 채용공고를 평가하고 적합도를 판단합니다.
     *
     * @param userLocation 사용자 희망 근무 지역 (미리 추출되어 전달됨, LazyInitializationException 방지)
     * @return 평가된 채용공고 리스트 (매칭 점수 및 추천 이유 포함)
     */
    private List<JobRecommendationResponse> evaluateJobsWithAI(
            Profile profile,
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis,
            List<SaraminJobListResponse.Jobs.Job> jobs,
            String userLocation) {

log.info("[JobRecommendationWorkflow] Evaluating {} jobs with OpenAI", jobs.size());

        // 사용자의 총 경력 연수 계산
        double userCareerYears = calculateTotalCareerYears(profile);
        log.info("[JobRecommendationWorkflow] User career years: {}", userCareerYears);

        // 사용자 학력 정보
        String userEducationLevel = profile.getEducationLevel();
        log.info("[JobRecommendationWorkflow] User education level: {}", userEducationLevel);

        // 1차 필터링: 경력 요구사항 필터링
        List<SaraminJobListResponse.Jobs.Job> careerFilteredJobs = jobs.stream()
                .filter(job -> isJobSuitableForCareerLevel(job, userCareerYears))
                .toList();

        int careerFiltered = jobs.size() - careerFilteredJobs.size();
        if (careerFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to career mismatch", careerFiltered);
        }

        // 2차 필터링: 학력 요구사항 필터링
        List<SaraminJobListResponse.Jobs.Job> educationFilteredJobs = careerFilteredJobs.stream()
                .filter(job -> isJobSuitableForEducationLevel(job, userEducationLevel))
                .toList();

        int educationFiltered = careerFilteredJobs.size() - educationFilteredJobs.size();
        if (educationFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to education mismatch", educationFiltered);
        }

        // 3차 필터링: 지역 필터링
        List<SaraminJobListResponse.Jobs.Job> locationFilteredJobs = educationFilteredJobs.stream()
                .filter(job -> isJobSuitableForLocation(job, userLocation))
                .toList();

        int locationFiltered = educationFilteredJobs.size() - locationFilteredJobs.size();
        if (locationFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to location mismatch", locationFiltered);
        }

        // 4차 필터링: 급여 조건 필터링
        Integer userDesiredMinSalary = extractDesiredMinSalary(profile);
        List<SaraminJobListResponse.Jobs.Job> filteredJobs = locationFilteredJobs.stream()
                .filter(job -> isJobSuitableForSalary(job, userDesiredMinSalary))
                .toList();

        int salaryFiltered = locationFilteredJobs.size() - filteredJobs.size();
        if (salaryFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to salary below expectation", salaryFiltered);
        }

        log.info("[JobRecommendationWorkflow] Total filtered: {} jobs (career: {}, education: {}, location: {}, salary: {}), remaining: {}",
                careerFiltered + educationFiltered + locationFiltered + salaryFiltered,
                careerFiltered, educationFiltered, locationFiltered, salaryFiltered, filteredJobs.size());

        if (filteredJobs.isEmpty()) {
            log.warn("[JobRecommendationWorkflow] No jobs remaining after filtering");
            return Collections.emptyList();
        }

        // 1. 사용자 프로필 컨텍스트 구성
        String userContext = buildUserContext(profile, ksaAnalysis);

        // 2. 채용공고 정보 구성 (필터링된 공고만 사용)
        String jobsContext = buildJobsContext(filteredJobs);

        // 3. OpenAI 프롬프트 생성
        String prompt = buildEvaluationPrompt(ncsOccupation, userContext, jobsContext);

        try {
            // 4. OpenAI API 호출 (Chat Completion)
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", "당신은 경력 컨설턴트이자 채용 전문가입니다."),
                    Map.of("role", "user", "content", prompt)
            );
            String aiResponse = openAiClient.generateChatCompletion(messages).block();

            // 5. AI 응답 파싱 (JSON 형식 기대)
            List<JobEvaluationDto> evaluations = parseAiEvaluationResponse(aiResponse);

            // 6. 평가 결과를 JobRecommendationResponse로 변환 (필터링된 공고만)
            return filteredJobs.stream()
                    .map(job -> {
                        JobEvaluationDto evaluation = findEvaluationForJob(job.id(), evaluations);
                        return buildJobRecommendation(job, evaluation);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[JobRecommendationWorkflow] Error during AI evaluation: {}", e.getMessage(), e);
            // 폴백: AI 평가 실패 시 기본 변환 (필터링된 공고만)
            return filteredJobs.stream()
                    .map(job -> buildJobRecommendation(job, null))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 사용자 프로필 컨텍스트 구성
     */
    private String buildUserContext(Profile profile, KsaAnalysisResponse ksaAnalysis) {
        StringBuilder context = new StringBuilder();

        context.append("## 사용자 프로필\n\n");

        // 기본 정보
        context.append("### 기본 정보\n");
        context.append("- 학력: ").append(profile.getEducationLevel()).append("\n");

        // 경력 사항
        if (profile.getResume() != null && profile.getResume().getCareers() != null
                && !profile.getResume().getCareers().isEmpty()) {
            context.append("\n### 경력 사항\n");
            profile.getResume().getCareers().forEach(career -> {
                context.append("- ").append(career.getCompanyName().getValue());
                if (career.getPeriod() != null) {
                    context.append(" (").append(formatPeriod(career.getPeriod())).append(")");
                }
                context.append("\n");
                if (career.getDepartment() != null) {
                    context.append("  부서: ").append(career.getDepartment()).append("\n");
                }
                if (career.getDescription() != null) {
                    context.append("  설명: ").append(career.getDescription()).append("\n");
                }
            });
        }

        // 학력 사항
        if (profile.getResume() != null && profile.getResume().getEducation() != null) {
            context.append("\n### 학력 사항\n");
            var edu = profile.getResume().getEducation();
            context.append("- ").append(edu.getSchool());
            if (edu.getPeriod() != null) {
                context.append(" (").append(formatPeriod(edu.getPeriod())).append(")");
            }
            context.append("\n");
            if (edu.getMajor() != null) {
                context.append("  전공: ").append(edu.getMajor()).append("\n");
            }
            if (edu.getGpa() != null) {
                context.append("  학점: ").append(edu.getGpa()).append("\n");
            }
        }

        // 보유 스킬
        if (profile.getProfileSkills() != null && !profile.getProfileSkills().isEmpty()) {
            context.append("\n### 보유 스킬\n");
            profile.getProfileSkills().forEach(ps ->
                context.append("- ").append(ps.getSkill().getName()).append("\n")
            );
        }

        // 자격증 (모든 자격증 포함)
        if (profile.getResume() != null && profile.getResume().getCertificates() != null
                && !profile.getResume().getCertificates().isEmpty()) {

            // 모든 자격증 포함 (유효성 검사 제거, OpenAI가 평가)
            var allCertificates = profile.getResume().getCertificates();

            context.append("\n### 자격증\n");
            allCertificates.forEach(cert ->
                context.append("- ").append(cert.getCertificate().getJmfldnm()).append("\n")
            );
        }

        // KSA 분석 결과
        if (ksaAnalysis != null) {
            context.append("\n### KSA 역량 분석 결과\n");
            context.append(ksaAnalysis.overallAssessment()).append("\n");
        }

        return context.toString();
    }

    /**
     * 채용공고 정보 컨텍스트 구성
     */
    private String buildJobsContext(List<SaraminJobListResponse.Jobs.Job> jobs) {
        StringBuilder context = new StringBuilder();
        context.append("## 채용공고 목록\n\n");

        for (int i = 0; i < jobs.size(); i++) {
            SaraminJobListResponse.Jobs.Job job = jobs.get(i);
            context.append("### 공고 ").append(i + 1).append(" (ID: ").append(job.id()).append(")\n");
            context.append("- 제목: ").append(job.position().title()).append("\n");
            context.append("- 회사: ").append(job.company().detail().name()).append("\n");

            if (job.position().jobCode() != null) {
                context.append("- 직무: ").append(job.position().jobCode().name()).append("\n");
            }

            if (job.position().experienceLevel() != null) {
                context.append("- 경력: ").append(job.position().experienceLevel().name()).append("\n");
            }

            if (job.position().requiredEducationLevel() != null) {
                context.append("- 학력: ").append(job.position().requiredEducationLevel().name()).append("\n");
            }

            if (job.position().location() != null) {
                context.append("- 지역: ").append(job.position().location().name()).append("\n");
            }

            if (job.keyword() != null && !job.keyword().isEmpty()) {
                context.append("- 키워드: ").append(job.keyword()).append("\n");
            }

            context.append("\n");
        }

        return context.toString();
    }

    /**
     * OpenAI 평가 프롬프트 생성
     */
    private String buildEvaluationPrompt(
            NcsOccupation ncsOccupation,
            String userContext,
            String jobsContext) {

        return """
                당신은 경력 컨설턴트이자 채용 전문가입니다.
                사용자의 프로필과 역량을 분석하여 각 채용공고와의 적합도를 평가해주세요.

                ## 목표 직무
                - NCS 코드: %s
                - 직무명: %s

                %s

                %s

                ## 평가 요청사항

                각 채용공고에 대해 다음을 평가해주세요:
                1. **매칭 점수 (0-100)**: 사용자 역량과 공고 요구사항의 적합도
                   - 경력/학력 요구사항 충족 여부
                   - 보유 스킬과 요구 스킬 일치도
                   - KSA 역량 분석 결과 반영
                   - 자격증 관련성

                2. **추천 이유**: 해당 공고를 추천하는 구체적인 이유 (1-2문장)
                   - 사용자의 강점이 어떻게 활용될 수 있는지
                   - 부족한 부분이 있다면 어떻게 보완할 수 있는지

                ## 출력 형식 (반드시 JSON 형식으로)

                ```json
                [
                  {
                    "jobId": "공고ID",
                    "matchScore": 85,
                    "recommendationReason": "Spring Boot와 MariaDB 경력을 직접 활용할 수 있는 백엔드 개발 포지션입니다. 신입 채용이므로 현재 역량으로 도전 가능합니다."
                  },
                  ...
                ]
                ```

                주의사항:
                - 점수는 정확하고 객관적으로 평가해주세요
                - 65점 미만은 적합하지 않은 공고입니다
                - 추천 이유는 구체적이고 개인화되어야 합니다
                """.formatted(ncsOccupation.getDutyCd(), ncsOccupation.getDutyNm(), userContext, jobsContext);
    }

    /**
     * AI 응답에서 평가 결과 파싱
     */
    private List<JobEvaluationDto> parseAiEvaluationResponse(String aiResponse) {
        try {
            // JSON 블록 추출 (```json ... ``` 형식 처리)
            String jsonContent = aiResponse;
            if (aiResponse.contains("```json")) {
                int startIdx = aiResponse.indexOf("```json") + 7;
                int endIdx = aiResponse.lastIndexOf("```");
                jsonContent = aiResponse.substring(startIdx, endIdx).trim();
            } else if (aiResponse.contains("```")) {
                int startIdx = aiResponse.indexOf("```") + 3;
                int endIdx = aiResponse.lastIndexOf("```");
                jsonContent = aiResponse.substring(startIdx, endIdx).trim();
            }

            return objectMapper.readValue(jsonContent, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("[JobRecommendationWorkflow] Failed to parse AI evaluation response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 특정 공고에 대한 평가 결과 찾기
     */
    private JobEvaluationDto findEvaluationForJob(String jobId, List<JobEvaluationDto> evaluations) {
        return evaluations.stream()
                .filter(eval -> eval.jobId().equals(jobId))
                .findFirst()
                .orElse(null);
    }

    /**
     * JobRecommendationResponse 빌드
     */
    private JobRecommendationResponse buildJobRecommendation(
            SaraminJobListResponse.Jobs.Job job,
            JobEvaluationDto evaluation) {

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
                .recommendationReason(evaluation != null ?
                        evaluation.recommendationReason() : "추천 이유를 생성할 수 없습니다.")
                .matchScore(evaluation != null ?
                        evaluation.matchScore() : 0)
                .build();
    }

    /**
     * Period를 사람이 읽기 쉬운 문자열로 변환
     */
    private String formatPeriod(Period period) {
        if (period == null) {
            return "";
        }

        String startStr = period.getStartDate() != null ?
                period.getStartDate().toString() : "미정";
        String endStr = period.getEndDate() != null ?
                period.getEndDate().toString() : "재직중";

        return startStr + " ~ " + endStr;
    }

    /**
     * AI 평가 결과 DTO
     */
    private record JobEvaluationDto(
            String jobId,
            Integer matchScore,
            String recommendationReason
    ) {}

    /**
     * 사용자의 총 경력 연수를 계산합니다.
     *
     * 계산 방식:
     * 1. Resume에서 모든 Career 정보를 가져옴
     * 2. 각 Career의 Period(시작일~종료일)를 기반으로 근무 기간 계산
     * 3. 종료일이 없는 경우(재직중) 현재 날짜를 종료일로 사용
     * 4. **겹치는 경력 기간을 병합하여 중복 제거 (Merge Overlapping Intervals)**
     * 5. 병합된 기간들의 총합을 계산하여 반환
     *
     * @param profile 사용자 프로필
     * @return 총 경력 연수 (년 단위, 소수점)
     */
    private double calculateTotalCareerYears(Profile profile) {
        if (profile.getResume() == null || profile.getResume().getCareers() == null
                || profile.getResume().getCareers().isEmpty()) {
            log.debug("[JobRecommendationWorkflow] No career information found");
            return 0.0;
        }

        List<Career> careers = profile.getResume().getCareers();

        // 1. 유효한 Career Period만 수집
        List<Period> periods = new ArrayList<>();
        for (Career career : careers) {
            if (career.getPeriod() == null || career.getPeriod().getStartDate() == null) {
                log.debug("[JobRecommendationWorkflow] Career period is null or has no start date, skipping");
                continue;
            }

            LocalDate startDate = career.getPeriod().getStartDate();
            LocalDate endDate = career.getPeriod().getEndDate();

            // 재직중인 경우 현재 날짜를 종료일로 사용
            if (endDate == null) {
                endDate = LocalDate.now();
                log.debug("[JobRecommendationWorkflow] Career is ongoing, using current date as end date");
            }

            periods.add(Period.of(startDate, endDate));
        }

        if (periods.isEmpty()) {
            log.debug("[JobRecommendationWorkflow] No valid career periods found");
            return 0.0;
        }

        // 2. 겹치는 기간 병합
        List<Period> mergedPeriods = mergeOverlappingPeriods(periods);

        // 3. 병합된 기간들의 총 연수 계산
        double totalYears = 0.0;
        for (Period period : mergedPeriods) {
            long daysBetween = ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate());
            double years = daysBetween / 365.0;
            totalYears += years;

            log.debug("[JobRecommendationWorkflow] Merged period: {} ~ {} = {} years",
                    period.getStartDate(), period.getEndDate(), String.format("%.2f", years));
        }

        log.info("[JobRecommendationWorkflow] Total career calculated (after merging overlaps): {} years (from {} merged periods)",
                String.format("%.2f", totalYears), mergedPeriods.size());
        return totalYears;
    }

    /**
     * 겹치는 경력 기간을 병합합니다 (Merge Overlapping Intervals Algorithm).
     *
     * 알고리즘:
     * 1. 시작일 기준으로 정렬
     * 2. 현재 구간과 다음 구간이 겹치는지 확인
     * 3. 겹치면 병합, 안 겹치면 현재 구간을 결과에 추가하고 다음으로 이동
     *
     * 예시:
     * - Input: [(2020-01-01, 2022-12-31), (2021-06-01, 2023-06-30)]
     * - Output: [(2020-01-01, 2023-06-30)]
     *
     * @param periods 경력 기간 리스트
     * @return 병합된 경력 기간 리스트
     */
    private List<Period> mergeOverlappingPeriods(List<Period> periods) {
        if (periods.size() <= 1) {
            return new ArrayList<>(periods);
        }

        // 1. 시작일 기준 정렬
        List<Period> sorted = new ArrayList<>(periods);
        sorted.sort(Comparator.comparing(Period::getStartDate));

        log.debug("[JobRecommendationWorkflow] Merging {} career periods", sorted.size());

        // 2. 겹치는 구간 병합
        List<Period> merged = new ArrayList<>();
        Period current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            Period next = sorted.get(i);

            // 겹치는지 확인: 현재 종료일 >= 다음 시작일
            if (current.getEndDate().isAfter(next.getStartDate()) ||
                    current.getEndDate().isEqual(next.getStartDate())) {
                // 겹침: 병합 (종료일을 더 늦은 날짜로)
                LocalDate mergedEndDate = current.getEndDate().isAfter(next.getEndDate())
                        ? current.getEndDate()
                        : next.getEndDate();
                current = Period.of(current.getStartDate(), mergedEndDate);
                log.debug("[JobRecommendationWorkflow] Merged overlapping periods: {} ~ {}",
                        current.getStartDate(), current.getEndDate());
            } else {
                // 겹치지 않음: 현재 구간을 결과에 추가하고 다음으로 이동
                merged.add(current);
                current = next;
            }
        }

        // 마지막 구간 추가
        merged.add(current);

        log.debug("[JobRecommendationWorkflow] Merged {} periods into {} non-overlapping periods",
                sorted.size(), merged.size());

        return merged;
    }

    /**
     * 채용공고가 사용자의 희망 근무 지역에 적합한지 판단합니다.
     *
     * 개선된 판단 기준 (SaraminRegion 활용):
     * 1. 사용자 지역 정보가 없으면: 모든 공고 허용 (보수적 접근)
     * 2. 채용공고 지역 정보가 없으면: 보수적으로 허용
     * 3. "원격근무", "재택근무", "전국", "해외": 항상 허용
     * 4. SaraminRegion 데이터베이스를 활용한 정확한 지역 매칭
     *    - 1차 지역 코드 (regionCode1) 기반 광역 단위 매칭
     *    - 예: "서울특별시" → 101000 → 강남구(101010), 강동구(101020) 모두 매칭
     *
     * @param job 채용공고
     * @param userLocation 사용자 지역 (예: "서울특별시", "경기도", "강남구", null)
     * @return 적합 여부
     */
    private boolean isJobSuitableForLocation(
            SaraminJobListResponse.Jobs.Job job,
            String userLocation) {

        // 1. 사용자 지역이 지정되지 않았으면 모든 공고 허용
        if (userLocation == null || userLocation.isBlank()) {
            log.debug("[JobRecommendationWorkflow] User location not specified, allowing all jobs");
            return true;
        }

        if (job.position() == null || job.position().location() == null
                || job.position().location().name() == null) {
            // 2. 공고 지역 정보가 없으면 보수적으로 허용
            log.debug("[JobRecommendationWorkflow] Job {} has no location info, allowing conservatively", job.id());
            return true;
        }

        String jobLocationName = job.position().location().name();
        log.debug("[JobRecommendationWorkflow] Checking job {} - job location: '{}', user location: '{}'",
                job.id(), jobLocationName, userLocation);

        // 3. 원격근무 / 재택근무 / 전국 / 해외는 항상 허용
        if (jobLocationName.contains("원격") || jobLocationName.contains("재택") ||
                jobLocationName.contains("전국") || jobLocationName.contains("해외")) {
            log.debug("[JobRecommendationWorkflow] Job is remote/flexible location, allowing");
            return true;
        }

        // 4. SaraminRegion 데이터베이스를 활용한 지역 매칭
        try {
            // 4-1. 사용자 지역으로 SaraminRegion 검색 (LIKE 검색)
            List<SaraminRegion> userRegions = saraminRegionRepository.findByNameContainingIgnoreCase(
                    normalizeLocationForSearch(userLocation)
            );

            if (userRegions.isEmpty()) {
                // 사용자 지역을 DB에서 찾을 수 없으면 폴백: 기본 문자열 매칭
                log.debug("[JobRecommendationWorkflow] User region '{}' not found in DB, using fallback string matching",
                        userLocation);
                return fallbackStringLocationMatching(jobLocationName, userLocation);
            }

            // 4-2. 채용공고 지역으로 SaraminRegion 검색
            List<SaraminRegion> jobRegions = saraminRegionRepository.findByNameContainingIgnoreCase(
                    normalizeLocationForSearch(jobLocationName)
            );

            if (jobRegions.isEmpty()) {
                // 공고 지역을 DB에서 찾을 수 없으면 폴백: 기본 문자열 매칭
                log.debug("[JobRecommendationWorkflow] Job location '{}' not found in DB, using fallback string matching",
                        jobLocationName);
                return fallbackStringLocationMatching(jobLocationName, userLocation);
            }

            // 4-3. 1차 지역 코드(regionCode1) 기반 광역 매칭
            // 사용자 지역과 공고 지역의 1차 지역 코드가 하나라도 일치하면 매칭
            Set<Integer> userRegionCodes1 = userRegions.stream()
                    .map(SaraminRegion::getRegionCode1)
                    .collect(Collectors.toSet());

            Set<Integer> jobRegionCodes1 = jobRegions.stream()
                    .map(SaraminRegion::getRegionCode1)
                    .collect(Collectors.toSet());

            boolean matches = userRegionCodes1.stream().anyMatch(jobRegionCodes1::contains);

            log.debug("[JobRecommendationWorkflow] Region code matching - user codes: {}, job codes: {}, matches: {}",
                    userRegionCodes1, jobRegionCodes1, matches);

            return matches;

        } catch (Exception e) {
            // 예외 발생 시 보수적으로 허용하고 로그 기록
            log.error("[JobRecommendationWorkflow] Error during region matching: {}, allowing conservatively",
                    e.getMessage(), e);
            return true;
        }
    }

    /**
     * 지역 검색을 위한 정규화
     * DB 검색 시 불필요한 접미사 제거
     *
     * 예시:
     * - "서울특별시" → "서울"
     * - "경기도" → "경기"
     * - "부산광역시" → "부산"
     */
    private String normalizeLocationForSearch(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }

        return location.trim()
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("특별자치시", "")
                .replace("특별자치도", "")
                .replace("도", "")
                .trim();
    }

    /**
     * 폴백: 기본 문자열 매칭 (DB에서 지역을 찾을 수 없을 때)
     *
     * @param jobLocation 공고 지역명
     * @param userLocation 사용자 지역명
     * @return 매칭 여부
     */
    private boolean fallbackStringLocationMatching(String jobLocation, String userLocation) {
        String normalizedUserLocation = normalizeLocationForSearch(userLocation);
        String normalizedJobLocation = normalizeLocationForSearch(jobLocation);

        boolean matches = normalizedJobLocation.contains(normalizedUserLocation) ||
                normalizedUserLocation.contains(normalizedJobLocation);

        log.debug("[JobRecommendationWorkflow] Fallback string matching - user: '{}', job: '{}', matches: {}",
                normalizedUserLocation, normalizedJobLocation, matches);

        return matches;
    }

    /**
     * 채용공고가 사용자의 학력 수준에 적합한지 판단합니다.
     *
     * 판단 기준:
     * 1. 채용공고의 requiredEducationLevel 필드 분석
     * 2. "학력무관": 모든 사용자에게 적합
     * 3. 사용자 학력이 요구사항보다 높거나 같으면 적합
     * 4. 학력 정보가 없는 경우: 보수적으로 적합하다고 판단
     *
     * @param job 채용공고
     * @param userEducationLevel 사용자 학력 (예: "대학교졸업(4년)")
     * @return 적합 여부
     */
    private boolean isJobSuitableForEducationLevel(
            SaraminJobListResponse.Jobs.Job job,
            String userEducationLevel) {

        if (job.position() == null || job.position().requiredEducationLevel() == null
                || job.position().requiredEducationLevel().name() == null) {
            // 학력 정보가 없으면 보수적으로 적합하다고 판단
            return true;
        }

        String requiredEducation = job.position().requiredEducationLevel().name();
        log.debug("[JobRecommendationWorkflow] Checking job {} - required education: '{}', user education: '{}'",
                job.id(), requiredEducation, userEducationLevel);

        // 1. 학력무관
        if (requiredEducation.contains("학력무관")) {
            return true;
        }

        // 2. 사용자 학력이 null인 경우 보수적으로 허용
        if (userEducationLevel == null || userEducationLevel.isBlank()) {
            log.debug("[JobRecommendationWorkflow] User education level is null/blank, allowing job conservatively");
            return true;
        }

        // 3. 학력 레벨 비교
        try {
            int userLevel = getEducationLevelCode(userEducationLevel);
            int requiredLevel = getEducationLevelCode(requiredEducation);

            boolean suitable = userLevel >= requiredLevel;
            log.debug("[JobRecommendationWorkflow] Education comparison - userLevel: {}, requiredLevel: {}, suitable: {}",
                    userLevel, requiredLevel, suitable);
            return suitable;

        } catch (Exception e) {
            // 파싱 실패 시 보수적으로 허용
            log.debug("[JobRecommendationWorkflow] Could not parse education levels, allowing job conservatively - error: {}",
                    e.getMessage());
            return true;
        }
    }

    /**
     * 학력 문자열을 학력 레벨 코드로 변환합니다.
     *
     * 학력 레벨 순서:
     * 0 = 학력무관
     * 1 = 고등학교졸업
     * 2 = 대학졸업(2,3년) / 전문대졸
     * 3 = 대학교졸업(4년) / 대졸
     * 4 = 석사졸업
     * 5 = 박사졸업
     *
     * @param educationText 학력 문자열
     * @return 학력 레벨 코드 (0~5)
     */
    private int getEducationLevelCode(String educationText) {
        if (educationText == null || educationText.isBlank()) {
            return 0; // 학력무관으로 처리
        }

        String normalized = educationText.trim();

        // 학력무관
        if (normalized.contains("학력무관") || normalized.contains("무관")) {
            return 0;
        }

        // 박사
        if (normalized.contains("박사")) {
            return 5;
        }

        // 석사
        if (normalized.contains("석사")) {
            return 4;
        }

        // 대학교졸업(4년) / 대졸 / 학사
        if (normalized.contains("대학교졸업") || normalized.contains("4년") ||
                normalized.contains("대졸") || normalized.contains("학사")) {
            return 3;
        }

        // 대학졸업(2,3년) / 전문대 / 초대졸
        if (normalized.contains("대학졸업") || normalized.contains("2,3년") || normalized.contains("2~3년") ||
                normalized.contains("전문대") || normalized.contains("초대졸")) {
            return 2;
        }

        // 고등학교졸업 / 고졸
        if (normalized.contains("고등학교") || normalized.contains("고졸")) {
            return 1;
        }

        // 파싱 실패: 기본값 0 (학력무관)으로 처리
        log.debug("[JobRecommendationWorkflow] Unknown education level format: '{}', treating as NO_REQUIREMENT",
                normalized);
        return 0;
    }

    /**
     * 채용공고가 사용자의 희망 최소 급여 조건에 적합한지 판단합니다.
     *
     * 판단 기준:
     * 1. 사용자가 희망 최소 급여를 설정하지 않은 경우 → 모든 공고 허용
     * 2. 채용공고의 급여 정보가 없는 경우 → 보수적으로 허용
     * 3. 급여 정보가 있는 경우 → 공고의 최소 급여가 사용자 희망보다 높거나 같으면 허용
     *
     * @param job 채용공고
     * @param userDesiredMinSalary 사용자 희망 최소 급여 (만원 단위, NULL이면 조건 없음)
     * @return 급여 조건이 적합한 경우 true, 아니면 false
     */
    private boolean isJobSuitableForSalary(
            SaraminJobListResponse.Jobs.Job job,
            Integer userDesiredMinSalary) {

        // 1. 사용자가 급여 조건을 설정하지 않은 경우 모든 공고 허용
        if (userDesiredMinSalary == null) {
            return true;
        }

        // 2. 채용공고에 급여 정보가 없는 경우 보수적으로 허용
        if (job.salary() == null || job.salary().name() == null || job.salary().name().isBlank()) {
            log.debug("[JobRecommendationWorkflow] Job {} has no salary info, allowing conservatively", job.id());
            return true;
        }

        String salaryText = job.salary().name();

        // 3. 특수 케이스: "회사내규에 따름", "면접 후 결정" 등 → 보수적으로 허용
        if (salaryText.contains("회사내규") || salaryText.contains("면접") ||
                salaryText.contains("추후") || salaryText.contains("협의")) {
            log.debug("[JobRecommendationWorkflow] Job {} has negotiable salary, allowing conservatively", job.id());
            return true;
        }

        // 4. 급여 파싱 및 비교
        try {
            Integer jobMinSalary = parseSalaryMin(salaryText);
            if (jobMinSalary == null) {
                // 파싱 실패 시 보수적으로 허용
                log.debug("[JobRecommendationWorkflow] Could not parse salary '{}', allowing conservatively", salaryText);
                return true;
            }

            boolean suitable = jobMinSalary >= userDesiredMinSalary;
            log.debug("[JobRecommendationWorkflow] Salary comparison - job min: {} 만원, user desired: {} 만원, suitable: {}",
                    jobMinSalary, userDesiredMinSalary, suitable);
            return suitable;

        } catch (Exception e) {
            // 예외 발생 시 보수적으로 허용
            log.debug("[JobRecommendationWorkflow] Error parsing salary '{}': {}, allowing conservatively",
                    salaryText, e.getMessage());
            return true;
        }
    }

    /**
     * 급여 문자열에서 최소 급여를 추출합니다.
     *
     * 예시:
     * - "3000만원~4000만원" → 3000
     * - "연봉 3500만원 이상" → 3500
     * - "4000만원" → 4000
     * - "3000만원 ~ 5000만원" → 3000
     *
     * @param salaryText 급여 문자열
     * @return 최소 급여 (만원 단위), 파싱 실패 시 null
     */
    private Integer parseSalaryMin(String salaryText) {
        if (salaryText == null || salaryText.isBlank()) {
            return null;
        }

        // 숫자만 추출하는 정규식
        Pattern pattern = Pattern.compile("(\\d{1,5})(?:만원|만|원)");
        Matcher matcher = pattern.matcher(salaryText);

        if (matcher.find()) {
            String firstNumber = matcher.group(1);
            return Integer.parseInt(firstNumber);
        }

        return null;
    }

    /**
     * 채용공고가 사용자의 경력 수준에 적합한지 판단합니다.
     *
     * 판단 기준:
     * 1. 채용공고의 experienceLevel 필드 분석
     * 2. "신입", "경력무관": 모든 사용자에게 적합
     * 3. "경력 X년↑", "경력 X~Y년": 사용자 경력이 요구사항을 충족하는 경우에만 적합
     * 4. 경력 요구사항을 파싱할 수 없는 경우: 보수적으로 적합하다고 판단
     *
     * @param job 채용공고
     * @param userCareerYears 사용자 총 경력 연수
     * @return 적합 여부
     */
    private boolean isJobSuitableForCareerLevel(
            SaraminJobListResponse.Jobs.Job job,
            double userCareerYears) {

        if (job.position() == null || job.position().experienceLevel() == null
                || job.position().experienceLevel().name() == null) {
            // 경력 정보가 없으면 보수적으로 적합하다고 판단
            return true;
        }

        String experienceLevel = job.position().experienceLevel().name();
        log.debug("[JobRecommendationWorkflow] Checking job {} - experience level: '{}'",
                job.id(), experienceLevel);

        // 1. 신입 또는 경력무관
        if (experienceLevel.contains("신입") || experienceLevel.contains("경력무관")) {
            return true;
        }

        // 2. "경력 X년↑" 패턴 (예: "경력 3년↑", "경력 5년 이상")
        Pattern minYearsPattern = Pattern.compile("경력\\s*(\\d+)\\s*년\\s*[↑이상]");
        Matcher minMatcher = minYearsPattern.matcher(experienceLevel);
        if (minMatcher.find()) {
            int requiredMinYears = Integer.parseInt(minMatcher.group(1));
            boolean suitable = userCareerYears >= requiredMinYears;
            log.debug("[JobRecommendationWorkflow] Min years required: {}, user has: {}, suitable: {}",
                    requiredMinYears, String.format("%.2f", userCareerYears), suitable);
            return suitable;
        }

        // 3. "경력 X~Y년" 패턴 (예: "경력 3~7년")
        Pattern rangePattern = Pattern.compile("경력\\s*(\\d+)\\s*~\\s*(\\d+)\\s*년");
        Matcher rangeMatcher = rangePattern.matcher(experienceLevel);
        if (rangeMatcher.find()) {
            int minYears = Integer.parseInt(rangeMatcher.group(1));
            int maxYears = Integer.parseInt(rangeMatcher.group(2));
            boolean suitable = userCareerYears >= minYears && userCareerYears <= maxYears;
            log.debug("[JobRecommendationWorkflow] Required range: {}-{} years, user has: {}, suitable: {}",
                    minYears, maxYears, String.format("%.2f", userCareerYears), suitable);
            return suitable;
        }

        // 4. 파싱할 수 없는 경우 보수적으로 적합하다고 판단
        log.debug("[JobRecommendationWorkflow] Could not parse experience level, allowing job");
        return true;
    }
}

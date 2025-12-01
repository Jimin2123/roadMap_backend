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
import com.shingu.roadmap.apis.saramin.repository.SaraminRegionRepository;
import com.shingu.roadmap.apis.openai.util.CareerCalculator;
import com.shingu.roadmap.apis.openai.util.JobMatchingUtil;
import com.shingu.roadmap.apis.openai.util.PeriodFormatter;
import com.shingu.roadmap.diagnosis.dto.response.JobRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
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
    private static final int MAX_PAGES = 20;         // 최대 탐색 페이지 수
    private static final int JOBS_PER_PAGE = 20;    // 페이지당 채용공고 수

    /**
     * 사용자 프로필, KSA 분석 결과, NCS 코드를 기반으로 AI가 채용공고를 추천합니다.
     *
     * 페이지네이션 전략:
     * 1. 1페이지부터 시작하여 Saramin API에서 채용공고 조회
     * 2. OpenAI를 사용하여 각 공고의 매칭 점수와 추천 이유 생성
     * 3. 매칭 점수 65점 이상인 공고만 수집
     * 4. 10개 이상 수집될 때까지 다음 페이지로 이동 (최대 20페이지)
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
     * 1. 페이지 번호 1부터 순차적으로 Saramin API 호출
     * 2. 각 페이지의 공고를 OpenAI로 평가
     * 3. 매칭 점수 65점 이상만 수집
     * 4. 10개 수집되거나 최대 20페이지에 도달할 때까지 반복
     */
    private Mono<List<JobRecommendationResponse>> collectQualifiedJobsWithPagination(
            Profile profile,
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis) {

        return Mono.fromCallable(() -> {
            List<JobRecommendationResponse> qualifiedJobs = new ArrayList<>();
            int currentPage = 1; // 페이지 번호는 1부터 시작
            int pagesExplored = 0;

            log.info("[JobRecommendationWorkflow] Starting pagination - target: {} jobs, maxPages: {}",
                    TARGET_JOB_COUNT, MAX_PAGES);

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
                            profile, ncsOccupation, ksaAnalysis, jobs);

                    // 매칭 점수 65점 이상만 필터링
                    List<JobRecommendationResponse> qualified = evaluatedJobs.stream()
                            .filter(job -> job.matchScore() != null && job.matchScore() >= MIN_MATCH_SCORE)
                            .toList();

                    qualifiedJobs.addAll(qualified);

                    log.info("[JobRecommendationWorkflow] Page {} evaluation complete - qualified: {}/{}, total collected: {}",
                            pagesExplored, qualified.size(), jobs.size(), qualifiedJobs.size());

                    // 다음 페이지로 이동 (1씩 증가)
                    currentPage++;

                    // 더 이상 페이지가 없으면 종료
                    if ((currentPage - 1) * JOBS_PER_PAGE >= totalJobs) {
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
     * @return 평가된 채용공고 리스트 (매칭 점수 및 추천 이유 포함)
     */
    private List<JobRecommendationResponse> evaluateJobsWithAI(
            Profile profile,
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis,
            List<SaraminJobListResponse.Jobs.Job> jobs) {

        log.info("[JobRecommendationWorkflow] Evaluating {} jobs with OpenAI", jobs.size());

        // 사용자의 총 경력 연수 계산
        double userCareerYears = CareerCalculator.calculateTotalCareerYears(profile);
        log.info("[JobRecommendationWorkflow] User career years: {}", userCareerYears);

        // 사용자 학력 정보
        String userEducationLevel = profile.getEducationLevel();
        log.info("[JobRecommendationWorkflow] User education level: {}", userEducationLevel);

        // 1차 필터링: 경력 요구사항 필터링
        List<SaraminJobListResponse.Jobs.Job> careerFilteredJobs = jobs.stream()
                .filter(job -> JobMatchingUtil.isJobSuitableForCareerLevel(job, userCareerYears))
                .toList();

        int careerFiltered = jobs.size() - careerFilteredJobs.size();
        if (careerFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to career mismatch", careerFiltered);
        }

        // 2차 필터링: 학력 요구사항 필터링
        List<SaraminJobListResponse.Jobs.Job> educationFilteredJobs = careerFilteredJobs.stream()
                .filter(job -> JobMatchingUtil.isJobSuitableForEducationLevel(job, userEducationLevel))
                .toList();

        int educationFiltered = careerFilteredJobs.size() - educationFilteredJobs.size();
        if (educationFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to education mismatch", educationFiltered);
        }

        // 3차 필터링: 급여 조건 필터링
        Integer userDesiredMinSalary = extractDesiredMinSalary(profile);
        List<SaraminJobListResponse.Jobs.Job> filteredJobs = educationFilteredJobs.stream()
                .filter(job -> JobMatchingUtil.isJobSuitableForSalary(job, userDesiredMinSalary))
                .toList();

        int salaryFiltered = educationFilteredJobs.size() - filteredJobs.size();
        if (salaryFiltered > 0) {
            log.info("[JobRecommendationWorkflow] Filtered out {} jobs due to salary below expectation", salaryFiltered);
        }

        log.info("[JobRecommendationWorkflow] Total filtered: {} jobs (career: {}, education: {}, salary: {}), remaining: {}",
                careerFiltered + educationFiltered + salaryFiltered,
                careerFiltered, educationFiltered, salaryFiltered, filteredJobs.size());

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
                    context.append(" (").append(PeriodFormatter.format(career.getPeriod())).append(")");
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
                context.append(" (").append(PeriodFormatter.format(edu.getPeriod())).append(")");
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
     * AI 평가 결과 DTO
     */
    private record JobEvaluationDto(
            String jobId,
            Integer matchScore,
            String recommendationReason
    ) {}
}

package com.shingu.roadmap.apis.careernet.service;

import com.shingu.roadmap.apis.careernet.config.CareerNetProperties;
import com.shingu.roadmap.apis.careernet.dto.request.CounselingCaseRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobEncyclopediaDetailRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobEncyclopediaListRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobInformationRequest;
import com.shingu.roadmap.apis.careernet.dto.response.CareerNetIntegratedResponse;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.CounselingCaseDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common.CounselingCaseSummaryRecord;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common.EnrichedCounselingCase;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.JobEncyclopediaDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.JobEncyclopediaListResponse;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common.JobSummaryRecord;
import com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoListResponse;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 커리어넷 통합 서비스
 * feature.md에 정의된 4단계 프로세스를 구현
 *
 * 1단계: NCS 직무 기반 CareerNet 코드 선택 (AI 지원)
 * 2단계: 직업 백과 조회 (코드 기반 검색 → 상세)
 * 3단계: 직업 정보 조회 (코드 기반 검색 → 상세)
 * 4단계: 진로 상담 사례 조회 (코드 기반 검색)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareerNetIntegrationService {

    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_COUNSELING_CASES = 5;
    private static final int DETAIL_FETCH_CONCURRENCY = 5;

    private final CareerNetService careerNetService;
    private final CareerNetProperties careerNetProperties;
    private final CareerNetCodeProvider careerNetCodeProvider;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    /**
     * NCS 직무에 대한 커리어넷 통합 정보 조회
     *
     * @param ncsOccupation NCS 직무 정보
     * @return 통합 조회 결과 (Mono)
     */
    public Mono<CareerNetIntegratedResponse> getIntegratedCareerInfo(NcsOccupation ncsOccupation) {
        long startTime = System.currentTimeMillis();
        // Thread-safe warnings list for concurrent reactive operations
        List<String> warnings = Collections.synchronizedList(new ArrayList<>());

        log.info("Starting integrated CareerNet query for NCS: {} - {}",
                ncsOccupation.getDutyCd(), ncsOccupation.getDutyNm());

        // 1단계: NCS 직무 기반 CareerNet 코드 선택
        return generateSearchCodes(ncsOccupation)
                .flatMap(searchCodes -> {
                    long step1Time = System.currentTimeMillis() - startTime;
                    log.debug("Step 1 completed in {}ms: Codes selected", step1Time);

                    // 2단계: 직업 백과 조회
                    return queryJobEncyclopedia(ncsOccupation, searchCodes.encyclopediaThemeCode(), searchCodes.encyclopediaAptitudeCode())
                            .flatMap(encyclopediaDetail -> {
                                long step2Time = System.currentTimeMillis() - startTime - step1Time;
                                log.debug("Step 2 completed in {}ms: Encyclopedia queried", step2Time);

                                // 3단계: 직업 정보 조회 (백과 정보 활용)
                                return queryJobInformation(encyclopediaDetail, searchCodes.jobInfoCategoryCode())
                                        .flatMap(jobInfoDetail -> {
                                            long step3Time = System.currentTimeMillis() - startTime - step1Time - step2Time;
                                            log.debug("Step 3 completed in {}ms: Job info queried", step3Time);
                                            long step4StartTime = System.currentTimeMillis();

                                            // 4단계 + 5단계: 진로 상담 사례 조회 (목록 + 상세)
                                            return queryCounselingCasesWithDetails(searchCodes.counselingGubunCode())
                                                    .onErrorResume(e -> {
                                                        log.warn("Steps 4-5 failed (counseling cases): {}", e.getMessage());
                                                        warnings.add("진로 상담 사례 조회 실패: " + e.getMessage());
                                                        return Mono.just(Collections.emptyList());
                                                    })
                                                    .map(counselingCases -> {
                                                        long step4And5Time = System.currentTimeMillis() - step4StartTime;
                                                        // Step 4 time approximated as 20% of total (list fetch)
                                                        // Step 5 time approximated as 80% of total (parallel detail fetches)
                                                        long step4Time = step4And5Time / 5;  // ~20%
                                                        long step5Time = step4And5Time - step4Time;  // ~80%
                                                        long totalTime = System.currentTimeMillis() - startTime;

                                                        log.info("All steps completed in {}ms for NCS: {}",
                                                                totalTime, ncsOccupation.getDutyCd());

                                                        // 통합 응답 생성
                                                        return CareerNetIntegratedResponse.builder()
                                                                .ncsCode(ncsOccupation.getDutyCd())
                                                                .ncsName(ncsOccupation.getDutyNm())
                                                                .searchCodes(searchCodes)
                                                                .encyclopediaDetail(encyclopediaDetail)
                                                                .jobInfoDetail(jobInfoDetail)
                                                                .counselingCases(counselingCases)
                                                                .metadata(CareerNetIntegratedResponse.ProcessingMetadata.builder()
                                                                        .totalProcessingTimeMs(totalTime)
                                                                        .stepTimings(CareerNetIntegratedResponse.StepTimings.builder()
                                                                                .step1CodeGenerationMs(step1Time)
                                                                                .step2EncyclopediaMs(step2Time)
                                                                                .step3JobInfoMs(step3Time)
                                                                                .step4CounselingListMs(step4Time)
                                                                                .step5CounselingDetailsMs(step5Time)
                                                                                .build())
                                                                        .success(true)
                                                                        .warnings(warnings)
                                                                        .build())
                                                                .build();
                                                    });
                                        })
                                        .onErrorResume(e -> {
                                            log.warn("Step 3 failed (job info): {}", e.getMessage());
                                            warnings.add("직업 정보 조회 실패: " + e.getMessage());

                                            // 4단계는 스킵하고 부분 결과 반환
                                            long totalTime = System.currentTimeMillis() - startTime;
                                            return Mono.just(CareerNetIntegratedResponse.builder()
                                                    .ncsCode(ncsOccupation.getDutyCd())
                                                    .ncsName(ncsOccupation.getDutyNm())
                                                    .searchCodes(searchCodes)
                                                    .encyclopediaDetail(encyclopediaDetail)
                                                    .jobInfoDetail(null)
                                                    .counselingCases(Collections.emptyList())
                                                    .metadata(CareerNetIntegratedResponse.ProcessingMetadata.builder()
                                                            .totalProcessingTimeMs(totalTime)
                                                            .success(false)
                                                            .warnings(warnings)
                                                            .build())
                                                    .build());
                                        });
                            })
                            .onErrorResume(e -> {
                                log.warn("Step 2 failed (encyclopedia): {}", e.getMessage());
                                warnings.add("직업 백과 조회 실패: " + e.getMessage());

                                // 3, 4, 5단계 진행 (백과 정보 없이)
                                return queryJobInformation(null, searchCodes.jobInfoCategoryCode())
                                        .flatMap(jobInfoDetail -> queryCounselingCasesWithDetails(searchCodes.counselingGubunCode())
                                                .map(counselingCases -> {
                                                    long totalTime = System.currentTimeMillis() - startTime;
                                                    return CareerNetIntegratedResponse.builder()
                                                            .ncsCode(ncsOccupation.getDutyCd())
                                                            .ncsName(ncsOccupation.getDutyNm())
                                                            .searchCodes(searchCodes)
                                                            .encyclopediaDetail(null)
                                                            .jobInfoDetail(jobInfoDetail)
                                                            .counselingCases(counselingCases)
                                                            .metadata(CareerNetIntegratedResponse.ProcessingMetadata.builder()
                                                                    .totalProcessingTimeMs(totalTime)
                                                                    .success(false)
                                                                    .warnings(warnings)
                                                                    .build())
                                                            .build();
                                                }))
                                        .onErrorResume(e2 -> {
                                            long totalTime = System.currentTimeMillis() - startTime;
                                            return Mono.just(CareerNetIntegratedResponse.builder()
                                                    .ncsCode(ncsOccupation.getDutyCd())
                                                    .ncsName(ncsOccupation.getDutyNm())
                                                    .searchCodes(searchCodes)
                                                    .encyclopediaDetail(null)
                                                    .jobInfoDetail(null)
                                                    .counselingCases(Collections.emptyList())
                                                    .metadata(CareerNetIntegratedResponse.ProcessingMetadata.builder()
                                                            .totalProcessingTimeMs(totalTime)
                                                            .success(false)
                                                            .errorMessage("모든 조회 단계 실패")
                                                            .warnings(warnings)
                                                            .build())
                                                    .build());
                                        });
                            });
                })
                .timeout(TOTAL_TIMEOUT)
                .onErrorResume(e -> {
                    long totalTime = System.currentTimeMillis() - startTime;
                    log.error("Integration service failed: {}", e.getMessage(), e);

                    return Mono.just(CareerNetIntegratedResponse.builder()
                            .ncsCode(ncsOccupation.getDutyCd())
                            .ncsName(ncsOccupation.getDutyNm())
                            .metadata(CareerNetIntegratedResponse.ProcessingMetadata.builder()
                                    .totalProcessingTimeMs(totalTime)
                                    .success(false)
                                    .errorMessage("통합 서비스 실패: " + e.getMessage())
                                    .build())
                            .build());
                });
    }

    /**
     * 1단계: NCS 직무 기반 CareerNet 코드 선택 (AI 지원)
     */
    private Mono<CareerNetIntegratedResponse.SearchCodes> generateSearchCodes(NcsOccupation ncsOccupation) {
        String encyclopediaCodesJson = careerNetCodeProvider.getEncyclopediaCodesJson();
        String jobInfoCodesJson = careerNetCodeProvider.getJobInfoCodesJson();

        String prompt = String.format("""
                NCS 직무 정보를 기반으로 가장 적합한 CareerNet API 검색 코드를 선택해주세요.

                NCS 직무 정보:
                - 코드: %s
                - 직무명: %s
                - 설명: %s

                직업 백과 코드 목록:
                %s

                직업 정보 코드 목록:
                %s

                응답 형식 (JSON):
                {
                  "encyclopediaThemeCode": "선택한 테마 코드 (themes의 code 값)",
                  "encyclopediaAptitudeCode": "선택한 적성 유형 코드 (aptitudeTypes의 code 값)",
                  "jobInfoCategoryCode": "선택한 직업 카테고리 코드 (jobCategories의 code 값)",
                  "counselingGubunCode": null
                }

                주의사항:
                - 반드시 제공된 코드 목록에서만 선택할 것
                - NCS 직무와 가장 관련성 높은 코드를 선택할 것
                - counselingGubunCode는 항상 null로 설정 (전체 조회)
                """,
                ncsOccupation.getDutyCd(),
                ncsOccupation.getDutyNm(),
                ncsOccupation.getDutyDef() != null ? ncsOccupation.getDutyDef() : "없음",
                encyclopediaCodesJson,
                jobInfoCodesJson
        );

        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .timeout(API_TIMEOUT)
                .map(response -> {
                    try {
                        String cleanedResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
                        Map<String, String> codeMap = objectMapper.readValue(cleanedResponse, new TypeReference<>() {});

                        return CareerNetIntegratedResponse.SearchCodes.builder()
                                .encyclopediaThemeCode(codeMap.get("encyclopediaThemeCode"))
                                .encyclopediaAptitudeCode(codeMap.get("encyclopediaAptitudeCode"))
                                .jobInfoCategoryCode(codeMap.get("jobInfoCategoryCode"))
                                .counselingGubunCode(codeMap.get("counselingGubunCode"))
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to parse AI code selection response", e);
                        throw new RuntimeException("AI 코드 선택 실패", e);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("AI code selection failed, using default codes", e);
                    // Fallback: IT/SW 관련 기본 코드 사용
                    return Mono.just(CareerNetIntegratedResponse.SearchCodes.builder()
                            .encyclopediaThemeCode("102421")  // IT/SW
                            .encyclopediaAptitudeCode("104740")  // IT관련전문직
                            .jobInfoCategoryCode("105098")  // 정보통신 연구개발직 및 공학기술직
                            .counselingGubunCode(null)
                            .build());
                });
    }

    /**
     * 2단계: 직업 백과 조회 (코드 기반 검색 → AI 최적 항목 선택 → 상세)
     */
    private Mono<JobEncyclopediaDetailResponse> queryJobEncyclopedia(NcsOccupation ncsOccupation, String themeCode, String aptitudeCode) {
        JobEncyclopediaListRequest listRequest = new JobEncyclopediaListRequest(
                careerNetProperties.getApiKey(),
                1,  // pageIndex
                null,  // searchJobNm - 키워드 검색 제거
                themeCode,
                aptitudeCode,  // searchAptdCodes
                null   // searchJobCd
        );

        log.debug("Job Encyclopedia search - themeCode: {}, aptitudeCode: {}", themeCode, aptitudeCode);

        return Mono.fromCallable(() -> careerNetService.getJobEncyclopediaList(listRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(API_TIMEOUT)
                .flatMap(listResponse -> {
                    if (listResponse == null || listResponse.jobs() == null || listResponse.jobs().isEmpty()) {
                        return Mono.error(new RuntimeException("직업 백과 목록이 비어있습니다"));
                    }

                    // AI 기반 최적 항목 선택
                    return selectBestJobFromList(ncsOccupation, listResponse.jobs())
                            .flatMap(selectedJob -> {
                                log.debug("AI selected job from encyclopedia: {} (jobCd: {})",
                                        selectedJob.jobNm(), selectedJob.jobCd());

                                // 상세 조회 (DTO 파라미터 이름은 seq이지만 실제로는 jobCd 값 전달)
                                JobEncyclopediaDetailRequest detailRequest = new JobEncyclopediaDetailRequest(
                                        careerNetProperties.getApiKey(),
                                        Integer.parseInt(selectedJob.jobCd())
                                );

                                return Mono.fromCallable(() -> careerNetService.getJobEncyclopediaDetail(detailRequest))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .timeout(API_TIMEOUT);
                            });
                });
    }

    /**
     * AI를 사용하여 직업 목록에서 NCS 직무와 가장 적합한 항목 선택
     */
    private Mono<JobSummaryRecord> selectBestJobFromList(NcsOccupation ncsOccupation, List<JobSummaryRecord> jobs) {
        // 단일 항목이면 바로 반환
        if (jobs.size() == 1) {
            log.debug("Only one job in list, selecting: {}", jobs.get(0).jobNm());
            return Mono.just(jobs.get(0));
        }

        // 직업 목록을 구조화된 텍스트로 포맷팅
        String jobListText = jobs.stream()
                .map(job -> String.format("- [%s] %s", job.jobCd(), job.jobNm()))
                .collect(java.util.stream.Collectors.joining("\n"));

        String prompt = String.format("""
                NCS 직무 정보와 직업 백과 목록을 비교하여 가장 적합한 직업을 선택해주세요.

                NCS 직무 정보:
                - 코드: %s
                - 직무명: %s
                - 설명: %s

                직업 백과 목록:
                %s

                응답 형식 (JSON):
                {
                  "selectedJobCd": "선택한 직업의 jobCd 값",
                  "reason": "선택 이유 (1-2문장)"
                }

                주의사항:
                - NCS 직무와 가장 관련성 높은 직업을 선택할 것
                - 반드시 위 목록에서만 선택할 것
                - jobCd 값을 정확히 응답할 것
                """,
                ncsOccupation.getDutyCd(),
                ncsOccupation.getDutyNm(),
                ncsOccupation.getDutyDef() != null ? ncsOccupation.getDutyDef() : "없음",
                jobListText
        );

        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .timeout(API_TIMEOUT)
                .map(response -> {
                    try {
                        String cleanedResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
                        Map<String, String> selectionMap = objectMapper.readValue(cleanedResponse, new TypeReference<>() {});
                        String selectedJobCd = selectionMap.get("selectedJobCd");
                        String reason = selectionMap.get("reason");

                        log.info("AI selection reason: {}", reason);

                        // jobCd로 직업 찾기
                        return jobs.stream()
                                .filter(job -> job.jobCd().equals(selectedJobCd))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("AI가 선택한 jobCd를 찾을 수 없습니다: " + selectedJobCd));
                    } catch (Exception e) {
                        log.error("Failed to parse AI job selection response", e);
                        throw new RuntimeException("AI 직업 선택 실패", e);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("AI job selection failed, using first item. Error: {}", e.getMessage());
                    // Fallback: 첫 번째 항목 반환
                    return Mono.just(jobs.get(0));
                });
    }

    /**
     * 3단계: 직업 정보 조회 (직업명 기반 검색 → 상세)
     */
    private Mono<JobInfoDetailResponse> queryJobInformation(JobEncyclopediaDetailResponse encyclopediaDetail, String categoryCode) {
        // 직업 백과에서 얻은 직업명으로 검색
        String jobNm = encyclopediaDetail != null && encyclopediaDetail.baseInfo() != null
                ? encyclopediaDetail.baseInfo().jobNm()
                : null;

        JobInformationRequest listRequest = new JobInformationRequest(
                careerNetProperties.getApiKey(),
                "api",
                "JOB",
                "job_dic_list",
                "json",
                null,  // pgubn
                categoryCode,  // category - 코드 기반 검색
                "1",   // thisPage
                "10",  // perPage
                jobNm,  // searchJobNm - 백과에서 얻은 직업명으로 검색
                null   // jobdicSeq
        );

        log.debug("Job Information search - categoryCode: {}, jobNm: {}", categoryCode, jobNm);

        return Mono.fromCallable(() -> careerNetService.getJobInfoList(listRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(API_TIMEOUT)
                .flatMap(listResponse -> {
                    log.debug("Job Information search - listResponse: {}", listResponse);
                    if (listResponse == null || listResponse.dataSearch().content() == null || listResponse.dataSearch().content().isEmpty()) {
                        return Mono.error(new RuntimeException("직업 정보 목록이 비어있습니다"));
                    }

                    // 첫 번째 항목 선택
                    String selectedJobdicSeq = listResponse.dataSearch().content().getFirst().jobdicSeq();
                    log.debug("Selected job info seq: {}", selectedJobdicSeq);

                    // 상세 조회
                    JobInformationRequest detailRequest = new JobInformationRequest(
                            careerNetProperties.getApiKey(),
                            "api",
                            "JOB_VIEW",
                            "job_dic_list",
                            "json",
                            null,
                            null,
                            null,
                            null,
                            null,
                            selectedJobdicSeq
                    );

                    return Mono.fromCallable(() -> careerNetService.getJobInfoDetail(detailRequest))
                            .subscribeOn(Schedulers.boundedElastic())
                            .timeout(API_TIMEOUT);
                });
    }

    /**
     * 4단계: 진로 상담 사례 조회 (코드 기반 검색)
     * 5단계: 각 사례의 상세 정보 병렬 조회
     */
    private Mono<List<EnrichedCounselingCase>> queryCounselingCasesWithDetails(String gubunCode) {
        CounselingCaseRequest request = new CounselingCaseRequest(
                careerNetProperties.getApiKey(),
                "api",
                "COUNSEL",
                "json",
                gubunCode,  // gubun - 코드 기반 검색 (null이면 전체 조회)
                null   // con_cd
        );

        log.debug("Counseling Cases search - gubunCode: {}", gubunCode);

        return Mono.fromCallable(() -> careerNetService.getCounselingCaseList(request))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(API_TIMEOUT)
                .flatMap(response -> {
                    if (response == null || response.content() == null || response.content().isEmpty()) {
                        log.debug("No counseling cases found");
                        return Mono.just(Collections.<EnrichedCounselingCase>emptyList());
                    }

                    // 상위 MAX_COUNSELING_CASES개의 사례만 선택
                    List<CounselingCaseSummaryRecord> summaries = response.content().stream()
                            .limit(MAX_COUNSELING_CASES)
                            .toList();

                    log.debug("Fetching details for {} counseling cases", summaries.size());

                    // 각 사례의 상세 정보를 병렬로 조회 (최대 DETAIL_FETCH_CONCURRENCY개 동시 실행)
                    return Flux.fromIterable(summaries)
                            .flatMap(this::fetchCounselingCaseDetail, DETAIL_FETCH_CONCURRENCY)
                            .collectList();
                })
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * 단일 상담 사례의 상세 정보 조회
     * 실패 시 요약 정보만 포함한 EnrichedCounselingCase 반환
     */
    private Mono<EnrichedCounselingCase> fetchCounselingCaseDetail(CounselingCaseSummaryRecord summary) {
        CounselingCaseRequest detailRequest = new CounselingCaseRequest(
                careerNetProperties.getApiKey(),
                "api",
                "COUNSEL",
                "json",
                null,  // gubun
                summary.code()  // con_cd - 특정 사례 조회
        );

        log.debug("Fetching detail for counseling case: {}", summary.code());

        return Mono.fromCallable(() -> careerNetService.getCounselingCaseDetail(detailRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(API_TIMEOUT)
                .map(detailResponse -> {
                    if (detailResponse != null && detailResponse.content() != null) {
                        log.debug("Detail fetched successfully for case: {}", summary.code());
                        return EnrichedCounselingCase.fromSummaryAndDetail(summary, detailResponse.content());
                    } else {
                        log.warn("Detail response empty for case: {}, using summary only", summary.code());
                        return EnrichedCounselingCase.fromSummary(summary);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Failed to fetch detail for case: {}, using summary only. Error: {}",
                            summary.code(), e.getMessage());
                    return Mono.just(EnrichedCounselingCase.fromSummary(summary));
                });
    }
}

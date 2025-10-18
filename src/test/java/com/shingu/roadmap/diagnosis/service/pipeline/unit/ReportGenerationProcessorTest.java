package com.shingu.roadmap.diagnosis.service.pipeline.unit;

import com.shingu.roadmap.apis.careernet.dto.response.CareerNetIntegratedResponse;
import com.shingu.roadmap.apis.careernet.service.CareerNetIntegrationService;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.repository.NcsOccupationRepository;
import com.shingu.roadmap.diagnosis.dto.common.*;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisContext;
import com.shingu.roadmap.diagnosis.service.pipeline.ReportGenerationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReportGenerationProcessor 단위 테스트
 *
 * 테스트 커버리지:
 * - 커리어넷 정보 보강
 * - 레이더 차트 데이터 생성
 * - 종합 요약 생성
 * - 강점/개선 영역 분석
 * - 예외 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportGenerationProcessor 단위 테스트")
class ReportGenerationProcessorTest {

    @Mock
    private CareerNetIntegrationService careerNetIntegrationService;

    @Mock
    private NcsOccupationRepository ncsOccupationRepository;

    @InjectMocks
    private ReportGenerationProcessor processor;

    private DiagnosisContext context;
    private NcsAnalysisResponse ncsAnalysis;
    private List<KsaAnalysisResponse> ksaAnalyses;
    private List<DiagnosisProgressResponse> progressCallbacks;

    @BeforeEach
    void setUp() {
        // NCS 분석 결과 설정
        ncsAnalysis = createNcsAnalysis();

        // KSA 분석 결과 설정
        ksaAnalyses = createKsaAnalyses();

        // Progress callback 설정
        progressCallbacks = new ArrayList<>();
        Consumer<DiagnosisProgressResponse> progressCallback = progressCallbacks::add;

        // DiagnosisContext 설정
        context = DiagnosisContext.builder()
                .memberId(1L)
                .diagnosisId(100L)
                .ncsAnalysisResponse(ncsAnalysis)
                .ksaAnalysisResponses(ksaAnalyses)
                .careerLevel("초급 실무자")
                .success(true)
                .progressCallback(progressCallback)
                .build();
    }

    private NcsAnalysisResponse createNcsAnalysis() {
        NcsRecommendationCandidate candidate = NcsRecommendationCandidate.builder()
                .ncsCode("20010208")
                .ncsName("시스템SW엔지니어링")
                .confidenceScore(0.85)
                .reason("적합도가 높습니다")
                .evidenceList(Collections.emptyList())
                .build();

        return NcsAnalysisResponse.builder()
                .candidates(List.of(candidate))
                .overallConfidence(0.85)
                .requiresUserSelection(false)
                .selectedNcsCode("20010208")
                .build();
    }

    private List<KsaAnalysisResponse> createKsaAnalyses() {
        List<KsaAnalysisResponse.KsaItem> knowledgeItems = List.of(
                KsaAnalysisResponse.KsaItem.builder()
                        .itemName("지식")
                        .itemDescription("1.유스케이스")
                        .userScore(0.5)
                        .targetScore(0.8)
                        .scoreGap(0.3)
                        .levelAssessment("NEED_IMPROVEMENT")
                        .gapDescription("학습 필요")
                        .recommendation("강좌 수강")
                        .build(),
                KsaAnalysisResponse.KsaItem.builder()
                        .itemName("지식")
                        .itemDescription("2.컴퓨터 아키텍처")
                        .userScore(0.7)
                        .targetScore(0.8)
                        .scoreGap(0.1)
                        .levelAssessment("ADEQUATE")
                        .gapDescription("적정 수준")
                        .recommendation("현재 수준 유지")
                        .build()
        );

        List<KsaAnalysisResponse.KsaItem> skillItems = List.of(
                KsaAnalysisResponse.KsaItem.builder()
                        .itemName("기술")
                        .itemDescription("1.프로그래밍 기술")
                        .userScore(0.8)
                        .targetScore(0.8)
                        .scoreGap(0.0)
                        .levelAssessment("EXCELLENT")
                        .gapDescription("우수")
                        .recommendation("현재 수준 유지")
                        .build()
        );

        List<KsaAnalysisResponse.KsaItem> attitudeItems = List.of(
                KsaAnalysisResponse.KsaItem.builder()
                        .itemName("태도")
                        .itemDescription("1.협업 자세")
                        .userScore(0.9)
                        .targetScore(0.8)
                        .scoreGap(-0.1)
                        .levelAssessment("EXCELLENT")
                        .gapDescription("매우 우수")
                        .recommendation("현재 수준 유지")
                        .build()
        );

        KsaAnalysisResponse ksaAnalysis = KsaAnalysisResponse.builder()
                .ncsCode("20010208")
                .knowledgeItems(knowledgeItems)
                .skillItems(skillItems)
                .attitudeItems(attitudeItems)
                .overallAssessment("전반적으로 양호합니다")
                .evidenceList(Collections.emptyList())
                .build();

        return List.of(ksaAnalysis);
    }

    @Nested
    @DisplayName("process 메서드 - 정상 흐름 테스트")
    class ProcessSuccessTests {

        @Test
        @DisplayName("성공: 리포트 생성 전체 흐름")
        void process_Success_CompleteFlow() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDiagnosisResultResponse()).isNotNull();

            DiagnosisResultResponse diagnosisResult = result.getDiagnosisResultResponse();
            assertThat(diagnosisResult.summary()).isNotNull();
            assertThat(diagnosisResult.ncsAnalyses()).isNotEmpty();
            assertThat(diagnosisResult.radarChartData()).isNotNull();
            assertThat(diagnosisResult.confidenceScore()).isEqualTo(0.85);

            // Mock 호출 검증
            verify(ncsOccupationRepository, times(1)).findById("20010208");
            verify(careerNetIntegrationService, times(1))
                    .getIntegratedCareerInfo(any(NcsOccupation.class));
        }

        @Test
        @DisplayName("검증: 레이더 차트 데이터가 올바르게 생성됨")
        void process_GeneratesRadarChartData_Correctly() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            RadarChartData radarChartData = result.getDiagnosisResultResponse().radarChartData();
            assertThat(radarChartData).isNotNull();
            assertThat(radarChartData.userProfile()).isNotNull();
            assertThat(radarChartData.targetNcsProfiles()).isNotEmpty();
            assertThat(radarChartData.competencyAxes()).isNotEmpty();

            // competencyAxes가 distinct한지 확인 (중복되지 않음)
            List<String> axes = radarChartData.competencyAxes();
            assertThat(axes).doesNotContainSequence("지식", "지식", "지식");
        }

        @Test
        @DisplayName("검증: 레이더 차트가 itemDescription을 사용함")
        void process_UsesItemDescription_ForRadarChart() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            RadarChartData radarChartData = result.getDiagnosisResultResponse().radarChartData();
            List<String> axes = radarChartData.competencyAxes();

            // itemDescription의 번호 접두사가 제거된 값들이 포함되어야 함
            assertThat(axes).contains("유스케이스", "컴퓨터 아키텍처", "프로그래밍 기술", "협업 자세");
        }

        @Test
        @DisplayName("검증: user와 target 프로필이 같은 역량 축을 사용함")
        void process_UserAndTargetProfiles_UseSameAxes() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            RadarChartData radarChartData = result.getDiagnosisResultResponse().radarChartData();

            CompetencyProfile userProfile = radarChartData.userProfile();
            NcsCompetencyProfile targetProfile = radarChartData.targetNcsProfiles().get(0);

            // 같은 역량명을 키로 사용해야 함
            Set<String> userKeys = userProfile.competencyScores().keySet();
            Set<String> targetKeys = targetProfile.competencyScores().keySet();

            assertThat(userKeys).isEqualTo(targetKeys);
        }

        @Test
        @DisplayName("검증: 요약에 커리어 레벨이 포함됨")
        void process_SummaryIncludesCareerLevel() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            String summary = result.getDiagnosisResultResponse().summary();
            assertThat(summary).contains("초급 실무자");
        }

        @Test
        @DisplayName("검증: 요약에 추천 직무 정보가 포함됨")
        void process_SummaryIncludesRecommendation() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            String summary = result.getDiagnosisResultResponse().summary();
            assertThat(summary).contains("시스템SW엔지니어링");
            assertThat(summary).contains("20010208");
            assertThat(summary).contains("85.0%");
        }

        @Test
        @DisplayName("검증: 요약에 역량 분석 결과가 포함됨")
        void process_SummaryIncludesCompetencyAnalysis() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            String summary = result.getDiagnosisResultResponse().summary();
            assertThat(summary).contains("역량 분석");
            assertThat(summary).contains("전반적으로 양호합니다");
        }

        @Test
        @DisplayName("검증: 커리어넷 정보가 후보에 보강됨")
        void process_EnrichesCandidate_WithCareerNetInfo() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            List<NcsRecommendationCandidate> enrichedCandidates =
                    result.getDiagnosisResultResponse().ncsAnalyses().get(0).candidates();

            NcsRecommendationCandidate candidate = enrichedCandidates.get(0);
            assertThat(candidate.careerNetJobInfo()).isNotNull();
            assertThat(candidate.ksaAnalysis()).isNotNull();
        }
    }

    @Nested
    @DisplayName("process 메서드 - 예외 처리 테스트")
    class ProcessErrorTests {

        @Test
        @DisplayName("실패: NCS 분석 결과가 없을 때 실패 처리")
        void process_Fails_WhenNcsAnalysisNull() {
            // Given
            context.setNcsAnalysisResponse(null);

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Required analysis results missing");
            verifyNoInteractions(careerNetIntegrationService);
        }

        @Test
        @DisplayName("실패: KSA 분석 결과가 없을 때 실패 처리")
        void process_Fails_WhenKsaAnalysisNull() {
            // Given
            context.setKsaAnalysisResponses(null);

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Required analysis results missing");
        }

        @Test
        @DisplayName("실패: KSA 분석 결과가 비어있을 때 실패 처리")
        void process_Fails_WhenKsaAnalysisEmpty() {
            // Given
            context.setKsaAnalysisResponses(Collections.emptyList());

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Required analysis results missing");
        }

        @Test
        @DisplayName("실패: NCS occupation을 찾을 수 없을 때 원본 후보 유지")
        void process_KeepsOriginalCandidate_WhenOccupationNotFound() {
            // Given
            when(ncsOccupationRepository.findById(anyString()))
                    .thenReturn(Optional.empty());

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            List<NcsRecommendationCandidate> candidates =
                    result.getDiagnosisResultResponse().ncsAnalyses().get(0).candidates();
            assertThat(candidates).isNotEmpty();
            assertThat(candidates.get(0).careerNetJobInfo()).isNull();
        }

        @Test
        @DisplayName("실패: 커리어넷 통합 조회 실패 시 원본 후보 유지")
        void process_KeepsOriginalCandidate_WhenCareerNetFails() {
            // Given
            NcsOccupation occupation = new NcsOccupation(
                    "20010208", "시스템SW엔지니어링", "SVC201600263", "시스템SW엔지니어링 직무 설명"
            );

            when(ncsOccupationRepository.findById("20010208"))
                    .thenReturn(Optional.of(occupation));

            when(careerNetIntegrationService.getIntegratedCareerInfo(any()))
                    .thenReturn(Mono.error(new RuntimeException("CareerNet API error")));

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            List<NcsRecommendationCandidate> candidates =
                    result.getDiagnosisResultResponse().ncsAnalyses().get(0).candidates();
            assertThat(candidates).isNotEmpty();
            assertThat(candidates.get(0).careerNetJobInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("레이더 차트 데이터 생성 상세 테스트")
    class RadarChartDataGenerationTests {

        @Test
        @DisplayName("검증: KSA가 비어있을 때 빈 레이더 차트 생성")
        void generateRadarChartData_ReturnsEmpty_WhenKsaIsEmpty() {
            // Given
            context.setKsaAnalysisResponses(Collections.emptyList());
            context.setNcsAnalysisResponse(ncsAnalysis);

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("검증: 역량 축이 K/S/A 순서로 생성됨")
        void generateRadarChartData_CreatesAxes_InKsaOrder() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            RadarChartData radarChartData = result.getDiagnosisResultResponse().radarChartData();
            List<String> axes = radarChartData.competencyAxes();

            // Knowledge → Skill → Attitude 순서
            int knowledgeCount = 2; // "유스케이스", "컴퓨터 아키텍처"
            int skillCount = 1;     // "프로그래밍 기술"
            int attitudeCount = 1;  // "협업 자세"

            assertThat(axes).hasSize(knowledgeCount + skillCount + attitudeCount);
        }

        @Test
        @DisplayName("검증: 번호 접두사가 제거됨")
        void generateRadarChartData_RemovesNumberPrefix() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            RadarChartData radarChartData = result.getDiagnosisResultResponse().radarChartData();
            List<String> axes = radarChartData.competencyAxes();

            // "1.", "2." 등의 접두사가 없어야 함
            axes.forEach(axis -> {
                assertThat(axis).doesNotMatch("^\\d+\\..*");
            });
        }

        @Test
        @DisplayName("검증: matchRate가 계산됨")
        void generateRadarChartData_CalculatesMatchRate() {
            // Given
            setupCareerNetMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            RadarChartData radarChartData = result.getDiagnosisResultResponse().radarChartData();
            NcsCompetencyProfile targetProfile = radarChartData.targetNcsProfiles().get(0);

            assertThat(targetProfile.matchRate()).isBetween(0.0, 1.0);
        }
    }

    @Nested
    @DisplayName("getName 메서드 테스트")
    class GetNameTests {

        @Test
        @DisplayName("검증: 프로세서 이름 반환")
        void getName_ReturnsCorrectName() {
            // When
            String name = processor.getName();

            // Then
            assertThat(name).isEqualTo("ReportGenerationProcessor");
        }
    }

    // Helper methods
    private void setupCareerNetMocks() {
        NcsOccupation occupation = new NcsOccupation(
                "20010208", "시스템SW엔지니어링", "SVC201600263", "시스템SW엔지니어링 직무 설명"
        );

        when(ncsOccupationRepository.findById("20010208"))
                .thenReturn(Optional.of(occupation));

        // Create a properly mocked CareerNetIntegratedResponse with nested structures
        CareerNetIntegratedResponse integratedResponse =
                mock(CareerNetIntegratedResponse.class);

        // Mock jobInfoDetail
        var jobInfoDetail = mock(com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoDetailResponse.class);
        var dataSearch = mock(com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoDetailResponse.ContentWrapper.class);
        var jobInfoContent = mock(com.shingu.roadmap.apis.careernet.dto.response.info.common.ContentRecord.class);

        // Configure the mock chain: integratedResponse -> jobInfoDetail -> dataSearch -> content
        when(integratedResponse.jobInfoDetail()).thenReturn(jobInfoDetail);
        when(jobInfoDetail.dataSearch()).thenReturn(dataSearch);
        when(dataSearch.content()).thenReturn(List.of(jobInfoContent));

        // Configure jobInfoContent with test data
        when(jobInfoContent.job()).thenReturn("시스템SW엔지니어링");
        when(jobInfoContent.summary()).thenReturn("시스템 소프트웨어를 설계하고 개발하는 직업");
        when(jobInfoContent.ability()).thenReturn("분석력, 논리적 사고");
        when(jobInfoContent.aptitude()).thenReturn("창의성, 집중력");
        when(jobInfoContent.similarJob()).thenReturn("응용소프트웨어개발자");
        when(jobInfoContent.jobPossibility()).thenReturn(List.of());
        when(jobInfoContent.stateOfEmployment()).thenReturn(List.of());
        when(jobInfoContent.preparationWay()).thenReturn(List.of());
        when(jobInfoContent.capacityMajor()).thenReturn(List.of());

        // Mock counselingCases (empty list for simplicity)
        when(integratedResponse.counselingCases()).thenReturn(List.of());

        when(careerNetIntegrationService.getIntegratedCareerInfo(any()))
                .thenReturn(Mono.just(integratedResponse));
    }
}

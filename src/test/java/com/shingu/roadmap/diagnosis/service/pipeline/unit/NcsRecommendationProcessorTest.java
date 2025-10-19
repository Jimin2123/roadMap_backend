package com.shingu.roadmap.diagnosis.service.pipeline.unit;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.dto.response.NcsCompUnitResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationResponse;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.diagnosis.dto.common.NcsRecommendationCandidate;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisContext;
import com.shingu.roadmap.diagnosis.service.pipeline.NcsRecommendationProcessor;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.domain.ProfileSkill;
import com.shingu.roadmap.member.domain.ProfileSkillId;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.enums.SkillProficiency;
import com.shingu.roadmap.resume.domain.Resume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NcsRecommendationProcessor 단위 테스트
 *
 * 테스트 커버리지:
 * - AI 기반 NCS 코드 추천
 * - NCS API 유효성 검증
 * - 능력단위 기반 후보 생성
 * - 신뢰도 계산
 * - 예외 처리
 * - 프로그레스 콜백
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NcsRecommendationProcessor 단위 테스트")
class NcsRecommendationProcessorTest {

    @Mock
    private OpenAiService openAiService;

    @Mock
    private NcsApiService ncsApiService;

    @InjectMocks
    private NcsRecommendationProcessor processor;

    private DiagnosisContext context;
    private Profile testProfile;
    private List<DiagnosisProgressResponse> progressCallbacks;

    @BeforeEach
    void setUp() {
        // Test profile 설정
        testProfile = Profile.builder()
                .id(1L)
                .profileSkills(createTestSkills())
                .resume(createTestResume())
                .build();

        // Progress callback 수집용 리스트
        progressCallbacks = new ArrayList<>();
        Consumer<DiagnosisProgressResponse> progressCallback = progressCallbacks::add;

        // DiagnosisContext 설정
        context = DiagnosisContext.builder()
                .memberId(1L)
                .diagnosisId(100L)
                .profile(testProfile)
                .success(true)
                .progressCallback(progressCallback)
                .build();
    }

    private Set<ProfileSkill> createTestSkills() {
        Skill java = Skill.builder().id(1L).name("Java").build();
        Skill spring = Skill.builder().id(2L).name("Spring Boot").build();

        ProfileSkill ps1 = ProfileSkill.builder()
                .skill(java)
                .proficiency(SkillProficiency.ADVANCED)
                .build();
        // Set ID manually for test (workaround for detached entity in Set.of())
        ReflectionTestUtils.setField(ps1, "id", new ProfileSkillId(1L, 1L));

        ProfileSkill ps2 = ProfileSkill.builder()
                .skill(spring)
                .proficiency(SkillProficiency.INTERMEDIATE)
                .build();
        // Set ID manually for test (workaround for detached entity in Set.of())
        ReflectionTestUtils.setField(ps2, "id", new ProfileSkillId(1L, 2L));

        return Set.of(ps1, ps2);
    }

    private Resume createTestResume() {
        return Resume.builder()
                .id(1L)
                .projects(Collections.emptyList())
                .build();
    }

    @Nested
    @DisplayName("process 메서드 - 정상 흐름 테스트")
    class ProcessSuccessTests {

        @Test
        @DisplayName("성공: AI 추천 → NCS 검증 → 후보 생성 정상 흐름")
        void process_Success_CompleteFlow() {
            // Given
            Set<String> recommendedCodes = Set.of("20010208", "20010303");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            // NCS 검증 - 유효한 occupation 반환
            NcsOccupation occupation1 = createNcsOccupation("20010208", "시스템SW엔지니어링");
            NcsOccupation occupation2 = createNcsOccupation("20010303", "IT기술지원");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occupation1, occupation2));

            // 능력단위 조회
            setupCompUnitMocks();

            // NCS occupation 정보 조회
            setupOccupationMocks();

            // AI 평가
            setupAiEvaluationMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getNcsAnalysisResponse()).isNotNull();

            NcsAnalysisResponse ncsAnalysis = result.getNcsAnalysisResponse();
            assertThat(ncsAnalysis.candidates()).isNotEmpty();
            assertThat(ncsAnalysis.candidates().size()).isLessThanOrEqualTo(5); // MAX_RECOMMENDATION_COUNT
            assertThat(ncsAnalysis.overallConfidence()).isGreaterThan(0.0);

            // 프로그레스 콜백 검증
            assertThat(progressCallbacks).isNotEmpty();

            // Mock 호출 검증
            verify(openAiService, times(1)).recommendNcsCodeUsingAssistant(testProfile);
            verify(ncsApiService, times(1)).filterValidNcsCodes(recommendedCodes);
        }

        @Test
        @DisplayName("검증: 신뢰도가 높을 때 자동 선택")
        void process_AutoSelectsNcs_WhenHighConfidence() {
            // Given
            Set<String> recommendedCodes = Set.of("20010208");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            NcsOccupation occupation = createNcsOccupation("20010208", "시스템SW엔지니어링");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occupation));

            setupCompUnitMocks();
            setupOccupationMocks();

            // 높은 신뢰도 (0.95) 설정
            // 최종 신뢰도 = (AI * 0.7) + (Rule * 0.3) >= 0.85이 되도록
            // (0.95 * 0.7) + (0.7 * 0.3) = 0.665 + 0.21 = 0.875 >= 0.85 ✓
            OpenAiService.NcsConfidenceEvaluation highConfidenceEval =
                    new OpenAiService.NcsConfidenceEvaluation(
                            0.95,
                            "EXCELLENT",
                            List.of("Java 전문가", "Spring Boot 경험"),
                            Collections.emptyList(),
                            "매우 적합합니다."
                    );
            when(openAiService.evaluateNcsMatchConfidence(anyString(), anyString(), anyList(), eq(testProfile)))
                    .thenReturn(Mono.just(highConfidenceEval));

            // resumeToText mock 추가 (rule-based confidence 계산에 필요)
            when(openAiService.resumeToText(any())).thenReturn("Java Spring Boot 프로젝트 경험");

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            NcsAnalysisResponse ncsAnalysis = result.getNcsAnalysisResponse();
            assertThat(ncsAnalysis.requiresUserSelection()).isFalse();
            assertThat(ncsAnalysis.selectedNcsCode()).isNotNull();
            assertThat(ncsAnalysis.selectedNcsCode()).isEqualTo("20010208");
        }

        @Test
        @DisplayName("검증: 신뢰도가 낮을 때 사용자 선택 요청")
        void process_RequiresUserSelection_WhenLowConfidence() {
            // Given
            Set<String> recommendedCodes = Set.of("20010208");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            NcsOccupation occupation = createNcsOccupation("20010208", "시스템SW엔지니어링");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occupation));

            setupCompUnitMocks();
            setupOccupationMocks();

            // 낮은 신뢰도 (0.6) 설정
            OpenAiService.NcsConfidenceEvaluation lowConfidenceEval =
                    new OpenAiService.NcsConfidenceEvaluation(
                            0.6,
                            "ADEQUATE",
                            List.of("Java 경험"),
                            List.of("시스템 아키텍처 경험 부족"),
                            "적정 수준입니다."
                    );
            when(openAiService.evaluateNcsMatchConfidence(anyString(), anyString(), anyList(), eq(testProfile)))
                    .thenReturn(Mono.just(lowConfidenceEval));

            // resumeToText mock 추가 (rule-based confidence 계산에 필요)
            when(openAiService.resumeToText(any())).thenReturn("Java Spring Boot 프로젝트 경험");

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            NcsAnalysisResponse ncsAnalysis = result.getNcsAnalysisResponse();
            assertThat(ncsAnalysis.requiresUserSelection()).isTrue();
            assertThat(ncsAnalysis.selectedNcsCode()).isNull();
        }

        @Test
        @DisplayName("검증: 후보들이 신뢰도 내림차순으로 정렬됨")
        void process_SortsCandidatesByConfidence() {
            // Given
            Set<String> recommendedCodes = Set.of("20010208", "20010303", "20010204");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            NcsOccupation occ1 = createNcsOccupation("20010208", "시스템SW엔지니어링");
            NcsOccupation occ2 = createNcsOccupation("20010303", "IT기술지원");
            NcsOccupation occ3 = createNcsOccupation("20010204", "DB엔지니어링");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occ1, occ2, occ3));

            setupCompUnitMocks();
            setupOccupationMocks();

            // 서로 다른 신뢰도 설정
            when(openAiService.evaluateNcsMatchConfidence(eq("20010208"), anyString(), anyList(), eq(testProfile)))
                    .thenReturn(Mono.just(createEvaluation(0.9)));
            when(openAiService.evaluateNcsMatchConfidence(eq("20010303"), anyString(), anyList(), eq(testProfile)))
                    .thenReturn(Mono.just(createEvaluation(0.7)));
            when(openAiService.evaluateNcsMatchConfidence(eq("20010204"), anyString(), anyList(), eq(testProfile)))
                    .thenReturn(Mono.just(createEvaluation(0.5)));

            when(openAiService.resumeToText(any())).thenReturn("");

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            List<NcsRecommendationCandidate> candidates = result.getNcsAnalysisResponse().candidates();
            assertThat(candidates).hasSize(3);

            // 신뢰도 내림차순 확인
            for (int i = 0; i < candidates.size() - 1; i++) {
                assertThat(candidates.get(i).confidenceScore())
                        .isGreaterThanOrEqualTo(candidates.get(i + 1).confidenceScore());
            }
        }
    }

    @Nested
    @DisplayName("process 메서드 - 예외 처리 테스트")
    class ProcessErrorTests {

        @Test
        @DisplayName("실패: Profile이 null일 때 예외 발생 및 실패 처리")
        void process_Fails_WhenProfileIsNull() {
            // Given
            context.setProfile(null);

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Required analysis results missing");
            verifyNoInteractions(openAiService);
            verifyNoInteractions(ncsApiService);
        }

        @Test
        @DisplayName("실패: AI 서비스 오류 시 실패 처리")
        void process_Fails_WhenAiServiceErrors() {
            // Given
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.error(new RuntimeException("OpenAI API error")));

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("AI 서비스 오류");
            verify(openAiService, times(1)).recommendNcsCodeUsingAssistant(testProfile);
            verifyNoInteractions(ncsApiService);
        }

        @Test
        @DisplayName("실패: AI가 빈 추천 결과를 반환할 때 실패 처리")
        void process_Fails_WhenAiReturnsEmpty() {
            // Given
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(Collections.emptySet()));

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("적합한 직무를 찾지 못했습니다");
            verify(openAiService, times(1)).recommendNcsCodeUsingAssistant(testProfile);
            verifyNoInteractions(ncsApiService);
        }

        @Test
        @DisplayName("실패: AI가 null을 반환할 때 실패 처리")
        void process_Fails_WhenAiReturnsNull() {
            // Given
            // Mono.just(null) causes NPE, use Mono.empty() or error instead
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.empty());

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("적합한 직무를 찾지 못했습니다");
        }

        @Test
        @DisplayName("실패: 모든 추천 NCS 코드가 유효하지 않을 때 실패 처리")
        void process_Fails_WhenAllCodesInvalid() {
            // Given
            Set<String> recommendedCodes = Set.of("99999999", "88888888");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Collections.emptySet());

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("유효하지 않습니다");
            verify(ncsApiService, times(1)).filterValidNcsCodes(recommendedCodes);
        }

        @Test
        @DisplayName("실패: 후보 생성에 모두 실패할 때 실패 처리")
        void process_Fails_WhenAllCandidatesBuildFail() {
            // Given
            Set<String> recommendedCodes = Set.of("20010208");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            NcsOccupation occupation = createNcsOccupation("20010208", "시스템SW엔지니어링");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occupation));

            // fetchAndRegisterNcsOccupation이 false 반환 (실패)
            when(ncsApiService.fetchAndRegisterNcsOccupation(anyString()))
                    .thenReturn(false);

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("직무 후보 생성에 실패했습니다");
        }
    }

    @Nested
    @DisplayName("프로그레스 콜백 테스트")
    class ProgressCallbackTests {

        @Test
        @DisplayName("검증: 프로그레스 콜백이 여러 단계에서 호출됨")
        void process_InvokesProgressCallback_MultipleTimes() {
            // Given
            Set<String> recommendedCodes = Set.of("20010208");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            NcsOccupation occupation = createNcsOccupation("20010208", "시스템SW엔지니어링");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occupation));

            setupCompUnitMocks();
            setupOccupationMocks();
            setupAiEvaluationMocks();

            // When
            processor.process(context);

            // Then
            assertThat(progressCallbacks).isNotEmpty();
            assertThat(progressCallbacks.size()).isGreaterThanOrEqualTo(3);

            // 진행률이 증가하는지 확인
            for (int i = 0; i < progressCallbacks.size() - 1; i++) {
                assertThat(progressCallbacks.get(i + 1).progressPercentage())
                        .isGreaterThanOrEqualTo(progressCallbacks.get(i).progressPercentage());
            }
        }

        @Test
        @DisplayName("검증: 콜백이 null일 때도 정상 동작")
        void process_WorksNormally_WhenCallbackIsNull() {
            // Given
            context.setProgressCallback(null);

            Set<String> recommendedCodes = Set.of("20010208");
            when(openAiService.recommendNcsCodeUsingAssistant(testProfile))
                    .thenReturn(Mono.just(recommendedCodes));

            NcsOccupation occupation = createNcsOccupation("20010208", "시스템SW엔지니어링");
            when(ncsApiService.filterValidNcsCodes(recommendedCodes))
                    .thenReturn(Set.of(occupation));

            setupCompUnitMocks();
            setupOccupationMocks();
            setupAiEvaluationMocks();

            // When & Then
            assertThatCode(() -> processor.process(context))
                    .doesNotThrowAnyException();
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
            assertThat(name).isEqualTo("NcsRecommendationProcessor");
        }
    }

    // Helper methods
    private NcsOccupation createNcsOccupation(String dutyCd, String dutyNm) {
        return new NcsOccupation(dutyCd, dutyNm, "SVC201600263", dutyNm + " 직무 설명");
    }

    private void setupCompUnitMocks() {
        NcsCompUnitResponse.NcsCompUnitItem compUnit1 = new NcsCompUnitResponse.NcsCompUnitItem(
                "20010208", "SVC201600263", "2001020801_15v1", "01",
                "시스템 요구사항 분석", "시스템 요구사항 분석 능력단위", 5
        );
        NcsCompUnitResponse.NcsCompUnitItem compUnit2 = new NcsCompUnitResponse.NcsCompUnitItem(
                "20010208", "SVC201600264", "2001020802_15v1", "02",
                "시스템 아키텍처 설계", "시스템 아키텍처 설계 능력단위", 6
        );
        NcsCompUnitResponse.ResponseInfo responseInfo = new NcsCompUnitResponse.ResponseInfo(
                "000", "정상", 1, "1", 2
        );
        NcsCompUnitResponse compUnitResponse = new NcsCompUnitResponse(
                List.of(compUnit1, compUnit2),
                responseInfo
        );

        when(ncsApiService.getNcsCompUnit(anyString()))
                .thenReturn(compUnitResponse);
    }

    private void setupOccupationMocks() {
        NcsOccupationResponse.NcsOccupationItem occItem =
                new NcsOccupationResponse.NcsOccupationItem(
                        "20010208",
                        "시스템SW엔지니어링",
                        "SVC201600263",
                        "시스템SW엔지니어링 직무 설명"
                );
        NcsOccupationResponse.NcsOccupationDataInfo dataInfo =
                new NcsOccupationResponse.NcsOccupationDataInfo(
                        "000", "정상", 1, 1, 1
                );
        NcsOccupationResponse occResponse = new NcsOccupationResponse(
                List.of(occItem),
                dataInfo
        );

        when(ncsApiService.fetchAndRegisterNcsOccupation(anyString()))
                .thenReturn(true);
        when(ncsApiService.getOccupation(anyString()))
                .thenReturn(occResponse);
    }

    private void setupAiEvaluationMocks() {
        OpenAiService.NcsConfidenceEvaluation evaluation = createEvaluation(0.85);

        when(openAiService.evaluateNcsMatchConfidence(
                anyString(), anyString(), anyList(), eq(testProfile)))
                .thenReturn(Mono.just(evaluation));

        when(openAiService.resumeToText(any())).thenReturn("Java Spring Boot 프로젝트 경험");
    }

    private OpenAiService.NcsConfidenceEvaluation createEvaluation(double confidence) {
        return new OpenAiService.NcsConfidenceEvaluation(
                confidence,
                "HIGH",
                List.of("Java 경험", "Spring Boot 활용"),
                Collections.emptyList(),
                "적합도가 높습니다."
        );
    }
}

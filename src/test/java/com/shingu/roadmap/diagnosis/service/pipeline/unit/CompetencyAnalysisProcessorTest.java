package com.shingu.roadmap.diagnosis.service.pipeline.unit;

import com.shingu.roadmap.apis.ncs.dto.response.NcsCompUnitResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsJobPositionResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsKsaResponse;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.diagnosis.dto.common.NcsRecommendationCandidate;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.diagnosis.service.pipeline.CompetencyAnalysisProcessor;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisContext;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.domain.ProfileSkill;
import com.shingu.roadmap.member.domain.ProfileSkillId;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.enums.SkillProficiency;
import com.shingu.roadmap.resume.domain.Period;
import com.shingu.roadmap.resume.domain.Project;
import com.shingu.roadmap.resume.domain.Resume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CompetencyAnalysisProcessor 단위 테스트
 *
 * 테스트 커버리지:
 * - 목표 NCS 코드 결정
 * - KSA 역량 분석
 * - AI 기반 평가 및 Fallback
 * - 커리어 레벨 진단
 * - 예외 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompetencyAnalysisProcessor 단위 테스트")
class CompetencyAnalysisProcessorTest {

    @Mock
    private NcsApiService ncsApiService;

    @Mock
    private OpenAiService openAiService;

    @InjectMocks
    private CompetencyAnalysisProcessor processor;

    private DiagnosisContext context;
    private Profile testProfile;
    private NcsAnalysisResponse ncsAnalysis;
    private List<DiagnosisProgressResponse> progressCallbacks;

    @BeforeEach
    void setUp() {
        // Test profile 설정
        testProfile = createTestProfile();

        // NCS 분석 결과 설정
        ncsAnalysis = createNcsAnalysis();

        // Progress callback 설정
        progressCallbacks = new ArrayList<>();
        Consumer<DiagnosisProgressResponse> progressCallback = progressCallbacks::add;

        // DiagnosisContext 설정
        context = DiagnosisContext.builder()
                .memberId(1L)
                .diagnosisId(100L)
                .profile(testProfile)
                .ncsAnalysisResponse(ncsAnalysis)
                .success(true)
                .progressCallback(progressCallback)
                .build();
    }

    private Profile createTestProfile() {
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

        Project project1 = Project.builder()
                .name("쇼핑몰 프로젝트")
                .role("백엔드 개발자")
                .period(Period.of(
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2024, 12, 31)
                ))
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .projects(List.of(project1))
                .build();

        return Profile.builder()
                .id(1L)
                .profileSkills(Set.of(ps1, ps2))
                .resume(resume)
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

    @Nested
    @DisplayName("process 메서드 - 정상 흐름 테스트")
    class ProcessSuccessTests {

        @Test
        @DisplayName("성공: KSA 분석 및 커리어 레벨 진단 정상 흐름")
        void process_Success_CompleteFlow() {
            // Given
            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getKsaAnalysisResponses()).isNotEmpty();
            assertThat(result.getCareerLevel()).isNotNull();

            List<KsaAnalysisResponse> ksaResponses = result.getKsaAnalysisResponses();
            assertThat(ksaResponses).hasSize(1);

            KsaAnalysisResponse ksaAnalysis = ksaResponses.get(0);
            assertThat(ksaAnalysis.ncsCode()).isEqualTo("20010208");
            assertThat(ksaAnalysis.overallAssessment()).isNotNull();

            // Mock 호출 검증
            verify(ncsApiService, times(1)).getNcsCompUnit("20010208");
            verify(ncsApiService, times(1)).getNcsKsa(eq("20010208"), anyString());
            verify(ncsApiService, times(1)).getNcsJobPosition("20010208");
        }

        @Test
        @DisplayName("검증: KSA 항목이 Knowledge, Skill, Attitude로 분류됨")
        void process_ClassifiesKsaItems_Correctly() {
            // Given
            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            KsaAnalysisResponse ksaAnalysis = result.getKsaAnalysisResponses().get(0);

            assertThat(ksaAnalysis.knowledgeItems()).isNotEmpty();
            assertThat(ksaAnalysis.skillItems()).isNotEmpty();
            assertThat(ksaAnalysis.attitudeItems()).isNotEmpty();

            // 각 항목이 적절한 데이터를 가지고 있는지 확인
            ksaAnalysis.knowledgeItems().forEach(item -> {
                assertThat(item.itemName()).isNotNull();
                assertThat(item.itemDescription()).isNotNull();
                assertThat(item.userScore()).isBetween(0.0, 1.0);
                assertThat(item.targetScore()).isBetween(0.0, 1.0);
            });
        }

        @Test
        @DisplayName("검증: 커리어 레벨이 경력 기간에 따라 결정됨")
        void process_DeterminesCareerLevel_BasedOnExperience() {
            // Given
            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            String careerLevel = result.getCareerLevel();
            assertThat(careerLevel).isIn(
                    "신입 / 초급",
                    "초급 실무자",
                    "중급 실무자",
                    "초급 관리자",
                    "중급 관리자"
            );
        }

        @Test
        @DisplayName("검증: AI 평가가 KSA 분석에 사용됨")
        void process_UsesAiEvaluation_ForKsaAnalysis() {
            // Given
            setupAiBasedKsaMocks(); // This now includes setupCompUnitAndKsaMocks()
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getKsaAnalysisResponses()).isNotEmpty();
            // Verify AI service was called for all 3 categories (Knowledge, Skill, Attitude)
            verify(openAiService, times(3)).analyzeKsaCompetency(
                    eq("20010208"), anyList(), eq(testProfile)
            );
        }
    }

    @Nested
    @DisplayName("목표 NCS 코드 결정 로직 테스트")
    class DetermineTargetNcsCodeTests {

        @Test
        @DisplayName("우선순위 1: 사용자가 직접 선택한 NCS 코드 사용")
        void process_UsesUserSelectedNcsCode_WhenAvailable() {
            // Given
            context.setUserSelectedNcsCode("99999999");
            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.getKsaAnalysisResponses().get(0).ncsCode())
                    .isEqualTo("99999999");
        }

        @Test
        @DisplayName("우선순위 2: 자동 선택된 NCS 코드 사용")
        void process_UsesSelectedNcsCode_WhenAvailable() {
            // Given
            context.setUserSelectedNcsCode(null);
            ncsAnalysis = NcsAnalysisResponse.builder()
                    .candidates(List.of(
                            NcsRecommendationCandidate.builder()
                                    .ncsCode("20010208")
                                    .ncsName("시스템SW엔지니어링")
                                    .confidenceScore(0.9)
                                    .reason("Test")
                                    .evidenceList(Collections.emptyList())
                                    .build()
                    ))
                    .overallConfidence(0.9)
                    .requiresUserSelection(false)
                    .selectedNcsCode("20010208")
                    .build();
            context.setNcsAnalysisResponse(ncsAnalysis);

            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.getKsaAnalysisResponses().get(0).ncsCode())
                    .isEqualTo("20010208");
        }

        @Test
        @DisplayName("우선순위 3: 첫 번째 후보 NCS 코드 사용")
        void process_UsesFirstCandidate_WhenNoSelection() {
            // Given
            context.setUserSelectedNcsCode(null);
            ncsAnalysis = NcsAnalysisResponse.builder()
                    .candidates(List.of(
                            NcsRecommendationCandidate.builder()
                                    .ncsCode("20010303")
                                    .ncsName("IT기술지원")
                                    .confidenceScore(0.75)
                                    .reason("Test")
                                    .evidenceList(Collections.emptyList())
                                    .build()
                    ))
                    .overallConfidence(0.75)
                    .requiresUserSelection(true)
                    .selectedNcsCode(null)
                    .build();
            context.setNcsAnalysisResponse(ncsAnalysis);

            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.getKsaAnalysisResponses().get(0).ncsCode())
                    .isEqualTo("20010303");
        }
    }

    @Nested
    @DisplayName("AI 평가 및 Fallback 처리 테스트")
    class AiEvaluationAndFallbackTests {

        @Test
        @DisplayName("성공: AI 평가 성공 시 AI 결과 사용")
        void process_UsesAiResults_WhenAiSucceeds() {
            // Given
            setupAiBasedKsaMocks(); // This now includes setupCompUnitAndKsaMocks()
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getKsaAnalysisResponses()).isNotEmpty();
            assertThat(result.getCareerLevel()).isNotNull();
            verify(openAiService, atLeastOnce())
                    .analyzeKsaCompetency(anyString(), anyList(), any());
        }

        @Test
        @DisplayName("Fallback: AI 평가 실패 시 규칙 기반 분석 사용")
        void process_UsesFallback_WhenAiFails() {
            // Given
            setupCompUnitAndKsaMocks();

            // AI 실패 시뮬레이션
            when(openAiService.analyzeKsaCompetency(anyString(), anyList(), any()))
                    .thenReturn(Mono.error(new RuntimeException("AI service error")));

            when(openAiService.resumeToText(any()))
                    .thenReturn("Java Spring Boot 프로젝트 경험");

            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getKsaAnalysisResponses()).isNotEmpty();

            // Fallback이 사용되었는지 확인
            KsaAnalysisResponse ksaAnalysis = result.getKsaAnalysisResponses().get(0);
            assertThat(ksaAnalysis.knowledgeItems()).isNotEmpty();
        }

        @Test
        @DisplayName("Fallback: AI가 빈 결과를 반환할 때 규칙 기반 분석 사용")
        void process_UsesFallback_WhenAiReturnsEmpty() {
            // Given
            setupCompUnitAndKsaMocks();

            // AI가 빈 결과 반환
            when(openAiService.analyzeKsaCompetency(anyString(), anyList(), any()))
                    .thenReturn(Mono.just(Collections.emptyMap()));

            when(openAiService.resumeToText(any()))
                    .thenReturn("Java Spring Boot");

            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getKsaAnalysisResponses()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("커리어 레벨 진단 테스트")
    class CareerLevelDiagnosisTests {

        @Test
        @DisplayName("검증: 경력 2년 미만 → 신입/초급")
        void diagnoseCareerLevel_ReturnsEntry_ForLessThan2Years() {
            // Given
            Profile newbieProfile = createProfileWithExperience(1); // 1년
            context.setProfile(newbieProfile);

            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.getCareerLevel()).isEqualTo("신입 / 초급");
        }

        @Test
        @DisplayName("검증: 경력 2~5년 → 초급 실무자")
        void diagnoseCareerLevel_ReturnsJunior_For2To5Years() {
            // Given
            Profile juniorProfile = createProfileWithExperience(3); // 3년
            context.setProfile(juniorProfile);

            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.getCareerLevel()).isIn("초급 실무자", "중급 실무자");
        }

        @Test
        @DisplayName("검증: 경력 5~10년 + 리드 역할 → 초급 관리자")
        void diagnoseCareerLevel_ReturnsManager_For5To10YearsWithLeadRole() {
            // Given
            Profile seniorProfile = createProfileWithExperienceAndRole(7, "리드 개발자");
            context.setProfile(seniorProfile);

            setupSuccessfulKsaMocks();
            setupCareerLevelMocks();

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.getCareerLevel()).isIn("초급 관리자", "중급 관리자");
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
            verifyNoInteractions(ncsApiService);
        }

        @Test
        @DisplayName("실패: NCS 후보가 비어있을 때 실패 처리")
        void process_Fails_WhenCandidatesEmpty() {
            // Given
            ncsAnalysis = NcsAnalysisResponse.builder()
                    .candidates(Collections.emptyList())
                    .overallConfidence(0.0)
                    .requiresUserSelection(false)
                    .selectedNcsCode(null)
                    .build();
            context.setNcsAnalysisResponse(ncsAnalysis);

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Required analysis results missing");
        }

        @Test
        @DisplayName("실패: 목표 NCS 코드를 결정할 수 없을 때 실패 처리 (candidates exist but no valid selection)")
        void process_Fails_WhenNoTargetNcsCode() {
            // Given
            // Create a candidate with null ncsCode to trigger determineTargetNcsCode returning null
            context.setUserSelectedNcsCode(null);
            ncsAnalysis = NcsAnalysisResponse.builder()
                    .candidates(List.of(
                            NcsRecommendationCandidate.builder()
                                    .ncsCode(null) // Invalid - will cause determineTargetNcsCode to fail
                                    .ncsName("테스트")
                                    .confidenceScore(0.5)
                                    .reason("Test")
                                    .evidenceList(Collections.emptyList())
                                    .build()
                    ))
                    .overallConfidence(0.5)
                    .requiresUserSelection(false)
                    .selectedNcsCode(null)
                    .build();
            context.setNcsAnalysisResponse(ncsAnalysis);

            // Note: No mock setup needed because validation fails before reaching API calls

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("분석 대상 직무가 선택되지 않았습니다");
        }

        @Test
        @DisplayName("실패: 능력단위 조회 실패 시 빈 KSA 결과 반환")
        void process_ReturnsEmptyKsa_WhenCompUnitFails() {
            // Given
            when(ncsApiService.getNcsCompUnit(anyString()))
                    .thenReturn(null);

            // No need for setupCareerLevelMocks() - test fails before reaching career level diagnosis

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("KSA 역량 분석에 실패했습니다");
        }

        @Test
        @DisplayName("실패: KSA 데이터 조회 실패 시 빈 KSA 결과 반환")
        void process_ReturnsEmptyKsa_WhenKsaDataFails() {
            // Given
            setupCompUnitMock();

            when(ncsApiService.getNcsKsa(anyString(), anyString()))
                    .thenReturn(null);

            // No need for setupCareerLevelMocks() - test fails before reaching career level diagnosis

            // When
            DiagnosisContext result = processor.process(context);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("KSA 역량 분석에 실패했습니다");
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
            assertThat(name).isEqualTo("CompetencyAnalysisProcessor");
        }
    }

    // Helper methods
    private void setupSuccessfulKsaMocks() {
        setupCompUnitAndKsaMocks();
        setupCareerLevelMocks();
        // Set up for fallback (rule-based analysis when AI fails)
        when(openAiService.resumeToText(any()))
                .thenReturn("Java Spring Boot 프로젝트 경험");
        // AI mock is set up when needed in specific tests
        when(openAiService.analyzeKsaCompetency(anyString(), anyList(), any()))
                .thenReturn(Mono.error(new RuntimeException("AI not configured - fallback should be used")));
    }

    private void setupCompUnitAndKsaMocks() {
        setupCompUnitMock();
        setupKsaMock();
    }

    private void setupCompUnitMock() {
        NcsCompUnitResponse.NcsCompUnitItem compUnit = new NcsCompUnitResponse.NcsCompUnitItem(
                "20010208",
                "SVC201600263",
                "2001020801_15v1",
                "01",
                "시스템 요구사항 분석",
                "시스템 요구사항을 분석하는 능력단위",
                5
        );
        NcsCompUnitResponse.ResponseInfo responseInfo = new NcsCompUnitResponse.ResponseInfo(
                "000", "정상", 1, "1", 1
        );
        NcsCompUnitResponse compUnitResponse = new NcsCompUnitResponse(
                List.of(compUnit),
                responseInfo
        );
        when(ncsApiService.getNcsCompUnit(anyString()))
                .thenReturn(compUnitResponse);
    }

    private void setupKsaMock() {
        List<NcsKsaResponse.NcsKsaItem> ksaItems = List.of(
                new NcsKsaResponse.NcsKsaItem(
                        "20010208", "SVC201600263", "2001020801_15v1", "01", "시스템 요구사항 분석",
                        1, "1.유스케이스", "K", "지식", "유스케이스에 대한 이해", 5
                ),
                new NcsKsaResponse.NcsKsaItem(
                        "20010208", "SVC201600263", "2001020801_15v1", "01", "시스템 요구사항 분석",
                        1, "2.컴퓨터 아키텍처", "K", "지식", "컴퓨터 아키텍처에 대한 이해", 5
                ),
                new NcsKsaResponse.NcsKsaItem(
                        "20010208", "SVC201600263", "2001020801_15v1", "01", "시스템 요구사항 분석",
                        1, "1.시스템 설계 기술", "S", "기술", "시스템을 설계할 수 있다", 5
                ),
                new NcsKsaResponse.NcsKsaItem(
                        "20010208", "SVC201600263", "2001020801_15v1", "01", "시스템 요구사항 분석",
                        1, "2.프로그래밍 기술", "S", "기술", "프로그래밍을 수행할 수 있다", 5
                ),
                new NcsKsaResponse.NcsKsaItem(
                        "20010208", "SVC201600263", "2001020801_15v1", "01", "시스템 요구사항 분석",
                        1, "1.문제 해결 의지", "A", "태도", "문제를 적극적으로 해결하려는 자세", 5
                ),
                new NcsKsaResponse.NcsKsaItem(
                        "20010208", "SVC201600263", "2001020801_15v1", "01", "시스템 요구사항 분석",
                        1, "2.협업 자세", "A", "태도", "팀원들과 협업하는 자세", 5
                )
        );

        NcsKsaResponse.ResponseInfo responseInfo = new NcsKsaResponse.ResponseInfo(
                "000", "정상", 1, "1", 6
        );
        NcsKsaResponse ksaResponse = new NcsKsaResponse(ksaItems, responseInfo);
        when(ncsApiService.getNcsKsa(anyString(), anyString()))
                .thenReturn(ksaResponse);
    }

    private void setupAiBasedKsaMocks() {
        // AI-based analysis still needs CompUnit and KSA data first
        setupCompUnitAndKsaMocks();

        // Resume text mock for confidence calculation
        when(openAiService.resumeToText(any()))
                .thenReturn("Java Spring Boot 프로젝트 경험");

        // AI results must match the actual KSA item names (gbnName from the mocked data)
        Map<String, OpenAiService.KsaEvaluationResult> aiResults = Map.of(
                "1.유스케이스", new OpenAiService.KsaEvaluationResult(0.7, "ADEQUATE", "적정 수준", "학습 추천"),
                "2.컴퓨터 아키텍처", new OpenAiService.KsaEvaluationResult(0.75, "ADEQUATE", "적정 수준", "학습 추천"),
                "1.시스템 설계 기술", new OpenAiService.KsaEvaluationResult(0.8, "EXCELLENT", "우수", "현재 수준 유지"),
                "2.프로그래밍 기술", new OpenAiService.KsaEvaluationResult(0.85, "EXCELLENT", "우수", "현재 수준 유지"),
                "1.문제 해결 의지", new OpenAiService.KsaEvaluationResult(0.75, "ADEQUATE", "적정 수준", "학습 추천"),
                "2.협업 자세", new OpenAiService.KsaEvaluationResult(0.8, "EXCELLENT", "우수", "현재 수준 유지")
        );

        when(openAiService.analyzeKsaCompetency(anyString(), anyList(), any()))
                .thenReturn(Mono.just(aiResults));
    }

    private void setupCareerLevelMocks() {
        NcsJobPositionResponse.JobData jobData = new NcsJobPositionResponse.JobData(
                Collections.emptyList(),
                Collections.emptyList()
        );
        NcsJobPositionResponse.ResponseInfo responseInfo = new NcsJobPositionResponse.ResponseInfo(
                "000", "정상"
        );
        NcsJobPositionResponse jobPosResponse = new NcsJobPositionResponse(
                jobData,
                responseInfo
        );
        when(ncsApiService.getNcsJobPosition(anyString()))
                .thenReturn(jobPosResponse);
    }

    private Profile createProfileWithExperience(int years) {
        Project project = Project.builder()
                .name("테스트 프로젝트")
                .role("개발자")
                .period(Period.of(
                        LocalDate.now().minusYears(years),
                        LocalDate.now()
                ))
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .projects(List.of(project))
                .build();

        return Profile.builder()
                .id(1L)
                .profileSkills(createTestProfile().getProfileSkills())
                .resume(resume)
                .build();
    }

    private Profile createProfileWithExperienceAndRole(int years, String role) {
        Project project = Project.builder()
                .name("테스트 프로젝트")
                .role(role)
                .period(Period.of(
                        LocalDate.now().minusYears(years),
                        LocalDate.now()
                ))
                .build();

        Resume resume = Resume.builder()
                .id(1L)
                .projects(List.of(project))
                .build();

        return Profile.builder()
                .id(1L)
                .profileSkills(createTestProfile().getProfileSkills())
                .resume(resume)
                .build();
    }
}

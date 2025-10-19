package com.shingu.roadmap.diagnosis.service.pipeline.unit;

import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisContext;
import com.shingu.roadmap.member.domain.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

/**
 * DiagnosisContext 단위 테스트
 *
 * 테스트 커버리지:
 * - Builder 패턴
 * - toBuilder 패턴
 * - Getter/Setter
 * - 데이터 불변성
 */
@DisplayName("DiagnosisContext 단위 테스트")
class DiagnosisContextTest {

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class BuilderTests {

        @Test
        @DisplayName("성공: Builder로 Context 생성")
        void builder_CreatesContext_Successfully() {
            // Given
            Long memberId = 1L;
            Long diagnosisId = 100L;
            Profile profile = Profile.builder().id(1L).build();
            boolean success = true;

            // When
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(memberId)
                    .diagnosisId(diagnosisId)
                    .profile(profile)
                    .success(success)
                    .build();

            // Then
            assertThat(context).isNotNull();
            assertThat(context.getMemberId()).isEqualTo(memberId);
            assertThat(context.getDiagnosisId()).isEqualTo(diagnosisId);
            assertThat(context.getProfile()).isEqualTo(profile);
            assertThat(context.isSuccess()).isEqualTo(success);
        }

        @Test
        @DisplayName("성공: 모든 필드를 설정한 Builder")
        void builder_WithAllFields_CreatesCompleteContext() {
            // Given
            Long memberId = 1L;
            Long diagnosisId = 100L;
            Profile profile = Profile.builder().id(1L).build();
            NcsAnalysisResponse ncsAnalysis = mock(NcsAnalysisResponse.class);
            List<KsaAnalysisResponse> ksaAnalyses = List.of(mock(KsaAnalysisResponse.class));
            String careerLevel = "초급 실무자";
            DiagnosisResultResponse resultResponse = mock(DiagnosisResultResponse.class);
            String userSelectedNcsCode = "20010208";
            String errorMessage = "Test error";
            boolean success = false;
            Consumer<DiagnosisProgressResponse> callback = progress -> {};

            // When
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(memberId)
                    .diagnosisId(diagnosisId)
                    .profile(profile)
                    .ncsAnalysisResponse(ncsAnalysis)
                    .ksaAnalysisResponses(ksaAnalyses)
                    .careerLevel(careerLevel)
                    .diagnosisResultResponse(resultResponse)
                    .userSelectedNcsCode(userSelectedNcsCode)
                    .errorMessage(errorMessage)
                    .success(success)
                    .progressCallback(callback)
                    .build();

            // Then
            assertThat(context.getMemberId()).isEqualTo(memberId);
            assertThat(context.getDiagnosisId()).isEqualTo(diagnosisId);
            assertThat(context.getProfile()).isEqualTo(profile);
            assertThat(context.getNcsAnalysisResponse()).isEqualTo(ncsAnalysis);
            assertThat(context.getKsaAnalysisResponses()).isEqualTo(ksaAnalyses);
            assertThat(context.getCareerLevel()).isEqualTo(careerLevel);
            assertThat(context.getDiagnosisResultResponse()).isEqualTo(resultResponse);
            assertThat(context.getUserSelectedNcsCode()).isEqualTo(userSelectedNcsCode);
            assertThat(context.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(context.isSuccess()).isEqualTo(success);
            assertThat(context.getProgressCallback()).isEqualTo(callback);
        }

        @Test
        @DisplayName("성공: 필드 일부만 설정한 Builder")
        void builder_WithPartialFields_CreatesPartialContext() {
            // Given & When
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .success(true)
                    .build();

            // Then
            assertThat(context.getMemberId()).isEqualTo(1L);
            assertThat(context.getDiagnosisId()).isEqualTo(100L);
            assertThat(context.isSuccess()).isTrue();
            assertThat(context.getProfile()).isNull();
            assertThat(context.getNcsAnalysisResponse()).isNull();
            assertThat(context.getKsaAnalysisResponses()).isNull();
        }
    }

    @Nested
    @DisplayName("toBuilder 패턴 테스트")
    class ToBuilderTests {

        @Test
        @DisplayName("성공: toBuilder로 기존 Context 복사 후 수정")
        void toBuilder_CopiesAndModifies_Successfully() {
            // Given
            DiagnosisContext original = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .success(true)
                    .errorMessage(null)
                    .build();

            // When
            DiagnosisContext modified = original.toBuilder()
                    .success(false)
                    .errorMessage("New error")
                    .build();

            // Then
            // 원본은 변경되지 않음
            assertThat(original.isSuccess()).isTrue();
            assertThat(original.getErrorMessage()).isNull();

            // 복사본은 변경됨
            assertThat(modified.isSuccess()).isFalse();
            assertThat(modified.getErrorMessage()).isEqualTo("New error");

            // 나머지 필드는 동일
            assertThat(modified.getMemberId()).isEqualTo(original.getMemberId());
            assertThat(modified.getDiagnosisId()).isEqualTo(original.getDiagnosisId());
        }

        @Test
        @DisplayName("검증: toBuilder는 불변성을 유지함")
        void toBuilder_MaintainsImmutability() {
            // Given
            DiagnosisContext original = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .careerLevel("초급")
                    .build();

            // When
            DiagnosisContext modified = original.toBuilder()
                    .careerLevel("중급")
                    .build();

            // Then
            assertThat(original.getCareerLevel()).isEqualTo("초급");
            assertThat(modified.getCareerLevel()).isEqualTo("중급");
            assertThat(original).isNotSameAs(modified);
        }
    }

    @Nested
    @DisplayName("Getter/Setter 테스트")
    class GetterSetterTests {

        @Test
        @DisplayName("성공: Setter로 필드 값 변경")
        void setter_ModifiesFields_Successfully() {
            // Given
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .build();

            // When
            Profile newProfile = Profile.builder().id(2L).build();
            context.setProfile(newProfile);

            NcsAnalysisResponse ncsAnalysis = mock(NcsAnalysisResponse.class);
            context.setNcsAnalysisResponse(ncsAnalysis);

            List<KsaAnalysisResponse> ksaAnalyses = Collections.emptyList();
            context.setKsaAnalysisResponses(ksaAnalyses);

            context.setCareerLevel("중급 실무자");
            context.setUserSelectedNcsCode("20010303");
            context.setErrorMessage("Test error");
            context.setSuccess(false);

            // Then
            assertThat(context.getProfile()).isEqualTo(newProfile);
            assertThat(context.getNcsAnalysisResponse()).isEqualTo(ncsAnalysis);
            assertThat(context.getKsaAnalysisResponses()).isEqualTo(ksaAnalyses);
            assertThat(context.getCareerLevel()).isEqualTo("중급 실무자");
            assertThat(context.getUserSelectedNcsCode()).isEqualTo("20010303");
            assertThat(context.getErrorMessage()).isEqualTo("Test error");
            assertThat(context.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("성공: Getter로 필드 값 조회")
        void getter_RetrievesFields_Successfully() {
            // Given
            Profile profile = Profile.builder().id(1L).build();
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .profile(profile)
                    .success(true)
                    .build();

            // When & Then
            assertThat(context.getMemberId()).isEqualTo(1L);
            assertThat(context.getDiagnosisId()).isEqualTo(100L);
            assertThat(context.getProfile()).isEqualTo(profile);
            assertThat(context.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("ProgressCallback 테스트")
    class ProgressCallbackTests {

        @Test
        @DisplayName("성공: ProgressCallback이 정상적으로 동작함")
        void progressCallback_Works_Successfully() {
            // Given
            final boolean[] callbackInvoked = {false};
            Consumer<DiagnosisProgressResponse> callback = progress -> {
                callbackInvoked[0] = true;
            };

            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .progressCallback(callback)
                    .build();

            // When
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(100L)
                    .progressPercentage(50)
                    .build();

            context.getProgressCallback().accept(progress);

            // Then
            assertThat(callbackInvoked[0]).isTrue();
        }

        @Test
        @DisplayName("성공: ProgressCallback이 null일 때 NullPointerException 발생 안 함")
        void progressCallback_IsNull_DoesNotThrowException() {
            // Given
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(1L)
                    .diagnosisId(100L)
                    .progressCallback(null)
                    .build();

            // When & Then
            assertThat(context.getProgressCallback()).isNull();
        }
    }

    // Helper method
    private <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}

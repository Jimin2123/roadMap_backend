package com.shingu.roadmap.diagnosis.service.unit;

import com.shingu.roadmap.diagnosis.domain.DiagnosisResult;
import com.shingu.roadmap.diagnosis.domain.DiagnosisResultData;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.dto.internal.MemberWithProfile;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.diagnosis.exception.DiagnosisAccessDeniedException;
import com.shingu.roadmap.diagnosis.exception.DiagnosisAlreadyInProgressException;
import com.shingu.roadmap.diagnosis.exception.DiagnosisNotFoundException;
import com.shingu.roadmap.diagnosis.repository.DiagnosisResultRepository;
import com.shingu.roadmap.diagnosis.service.DiagnosisEmitterManager;
import com.shingu.roadmap.diagnosis.service.DiagnosisService;
import com.shingu.roadmap.diagnosis.service.DiagnosisStateService;
import com.shingu.roadmap.diagnosis.service.pipeline.CompetencyAnalysisProcessor;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisContext;
import com.shingu.roadmap.diagnosis.service.pipeline.NcsRecommendationProcessor;
import com.shingu.roadmap.diagnosis.service.pipeline.ReportGenerationProcessor;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * DiagnosisService 단위 테스트
 *
 * 테스트 커버리지:
 * - 새 진단 생성
 * - 진단 결과 조회
 * - 진단 상태 업데이트
 * - 소유권 검증
 * - 예외 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosisService 단위 테스트")
class DiagnosisServiceTest {

    @Mock
    private DiagnosisResultRepository diagnosisResultRepository;

    @Mock
    private NcsRecommendationProcessor ncsRecommendationProcessor;

    @Mock
    private CompetencyAnalysisProcessor competencyAnalysisProcessor;

    @Mock
    private ReportGenerationProcessor reportGenerationProcessor;

    @Mock
    private DiagnosisEmitterManager emitterManager;

    @Mock
    private DiagnosisStateService diagnosisStateService;

    @InjectMocks
    private DiagnosisService diagnosisService;

    private Member testMember;
    private Profile testProfile;
    private DiagnosisResult testDiagnosis;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .name("testuser")
                .role("USER")
                .build();
        ReflectionTestUtils.setField(testMember, "id", 1L);

        testProfile = Profile.builder()
                .member(testMember)
                .build();
        ReflectionTestUtils.setField(testProfile, "id", 1L);

        testDiagnosis = DiagnosisResult.createPending(testMember.getId());
    }

    @Nested
    @DisplayName("createNewDiagnosis 메서드 테스트")
    class CreateNewDiagnosisTests {

        @Test
        @DisplayName("성공: 진행 중인 진단이 없을 때 새 진단 생성")
        void createNewDiagnosis_Success_WhenNoInProgressDiagnosis() {
            // Given
            Long memberId = 1L;
            when(diagnosisResultRepository.findInProgressDiagnosisWithLock(
                    eq(memberId),
                    anyList()))
                    .thenReturn(Optional.empty());

            DiagnosisResult savedDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(savedDiagnosis, "id", 100L);
            when(diagnosisResultRepository.save(any(DiagnosisResult.class)))
                    .thenReturn(savedDiagnosis);

            // When
            Long diagnosisId = diagnosisService.createNewDiagnosis(memberId);

            // Then
            assertThat(diagnosisId).isEqualTo(100L);
            verify(diagnosisResultRepository, times(1))
                    .findInProgressDiagnosisWithLock(eq(memberId), anyList());
            verify(diagnosisResultRepository, times(2))
                    .save(any(DiagnosisResult.class)); // PENDING 생성 + IN_PROGRESS 전환
        }

        @Test
        @DisplayName("실패: 이미 진행 중인 진단이 있을 때 예외 발생")
        void createNewDiagnosis_ThrowsException_WhenDiagnosisInProgress() {
            // Given
            Long memberId = 1L;
            DiagnosisResult existingDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(existingDiagnosis, "id", 50L);
            existingDiagnosis.startDiagnosis();

            when(diagnosisResultRepository.findInProgressDiagnosisWithLock(
                    eq(memberId),
                    anyList()))
                    .thenReturn(Optional.of(existingDiagnosis));

            // When & Then
            assertThatThrownBy(() -> diagnosisService.createNewDiagnosis(memberId))
                    .isInstanceOf(DiagnosisAlreadyInProgressException.class);

            verify(diagnosisResultRepository, times(1))
                    .findInProgressDiagnosisWithLock(eq(memberId), anyList());
            verify(diagnosisResultRepository, never())
                    .save(any(DiagnosisResult.class));
        }

        @Test
        @DisplayName("검증: 비관적 락이 올바르게 사용됨")
        void createNewDiagnosis_UsesPessimisticLock() {
            // Given
            Long memberId = 1L;
            when(diagnosisResultRepository.findInProgressDiagnosisWithLock(
                    eq(memberId),
                    anyList()))
                    .thenReturn(Optional.empty());

            DiagnosisResult savedDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(savedDiagnosis, "id", 100L);
            when(diagnosisResultRepository.save(any(DiagnosisResult.class)))
                    .thenReturn(savedDiagnosis);

            // When
            diagnosisService.createNewDiagnosis(memberId);

            // Then
            ArgumentCaptor<List<DiagnosisStatus>> statusListCaptor = ArgumentCaptor.forClass(List.class);
            verify(diagnosisResultRepository).findInProgressDiagnosisWithLock(
                    eq(memberId),
                    statusListCaptor.capture()
            );

            List<DiagnosisStatus> capturedStatuses = statusListCaptor.getValue();
            assertThat(capturedStatuses).contains(
                    DiagnosisStatus.IN_PROGRESS,
                    DiagnosisStatus.PENDING,
                    DiagnosisStatus.AWAITING_USER_INPUT
            );
        }

        @Test
        @DisplayName("검증: 진단 생성 직후 즉시 IN_PROGRESS로 전환")
        void createNewDiagnosis_TransitionsToInProgressImmediately() {
            // Given
            Long memberId = 1L;
            when(diagnosisResultRepository.findInProgressDiagnosisWithLock(
                    eq(memberId),
                    anyList()))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<DiagnosisResult> diagnosisCaptor = ArgumentCaptor.forClass(DiagnosisResult.class);
            DiagnosisResult savedDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(savedDiagnosis, "id", 100L);
            when(diagnosisResultRepository.save(diagnosisCaptor.capture()))
                    .thenReturn(savedDiagnosis);

            // When
            diagnosisService.createNewDiagnosis(memberId);

            // Then
            List<DiagnosisResult> savedDiagnoses = diagnosisCaptor.getAllValues();
            assertThat(savedDiagnoses).hasSize(2);
            assertThat(savedDiagnoses.get(0).getStatus()).isEqualTo(DiagnosisStatus.PENDING);
            assertThat(savedDiagnoses.get(1).getStatus()).isEqualTo(DiagnosisStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("findDiagnosisResult 메서드 테스트")
    class FindDiagnosisResultTests {

        @Test
        @DisplayName("성공: 완료된 진단 결과 조회")
        void findDiagnosisResult_Success_WhenDiagnosisCompleted() {
            // Given
            Long diagnosisId = 100L;
            Long memberId = 1L;

            DiagnosisResult completedDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(completedDiagnosis, "id", diagnosisId);
            completedDiagnosis.startDiagnosis();

            // Mock result data
            DiagnosisResultData resultData = DiagnosisResultData.builder()
                    .summary("Test summary")
                    .ncsAnalyses(Collections.emptyList())
                    .confidenceScore(0.85)
                    .build();
            completedDiagnosis.completeDiagnosis(resultData);

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(completedDiagnosis));

            // When
            DiagnosisResultResponse result = diagnosisService.findDiagnosisResult(diagnosisId, memberId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.diagnosisId()).isEqualTo(diagnosisId);
            assertThat(result.summary()).isEqualTo("Test summary");
            assertThat(result.confidenceScore()).isEqualTo(0.85);
            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 진단 ID로 조회 시 예외 발생")
        void findDiagnosisResult_ThrowsException_WhenDiagnosisNotFound() {
            // Given
            Long diagnosisId = 999L;
            Long memberId = 1L;

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> diagnosisService.findDiagnosisResult(diagnosisId, memberId))
                    .isInstanceOf(DiagnosisNotFoundException.class);

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }

        @Test
        @DisplayName("실패: 소유자가 아닌 사용자가 조회 시 예외 발생")
        void findDiagnosisResult_ThrowsException_WhenNotOwner() {
            // Given
            Long diagnosisId = 100L;
            Long ownerId = 1L;
            Long otherUserId = 2L;

            DiagnosisResult completedDiagnosis = DiagnosisResult.createPending(ownerId);
            ReflectionTestUtils.setField(completedDiagnosis, "id", diagnosisId);
            completedDiagnosis.startDiagnosis();

            DiagnosisResultData resultData = DiagnosisResultData.builder()
                    .summary("Test")
                    .ncsAnalyses(Collections.emptyList())
                    .confidenceScore(0.85)
                    .build();
            completedDiagnosis.completeDiagnosis(resultData);

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(completedDiagnosis));

            // When & Then
            assertThatThrownBy(() -> diagnosisService.findDiagnosisResult(diagnosisId, otherUserId))
                    .isInstanceOf(DiagnosisAccessDeniedException.class);

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }

        @Test
        @DisplayName("실패: 삭제된 진단 조회 시 예외 발생")
        void findDiagnosisResult_ThrowsException_WhenDiagnosisDeleted() {
            // Given
            Long diagnosisId = 100L;
            Long memberId = 1L;

            DiagnosisResult deletedDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(deletedDiagnosis, "id", diagnosisId);
            // Set status to FAILED to simulate a deleted/failed diagnosis
            ReflectionTestUtils.setField(deletedDiagnosis, "status", DiagnosisStatus.FAILED);

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(deletedDiagnosis));

            // When & Then
            assertThatThrownBy(() -> diagnosisService.findDiagnosisResult(diagnosisId, memberId))
                    .isInstanceOf(DiagnosisNotFoundException.class)
                    .hasMessageContaining("삭제된 진단 결과입니다");

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }

        @Test
        @DisplayName("실패: 완료되지 않은 진단 조회 시 예외 발생")
        void findDiagnosisResult_ThrowsException_WhenDiagnosisNotCompleted() {
            // Given
            Long diagnosisId = 100L;
            Long memberId = 1L;

            DiagnosisResult inProgressDiagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(inProgressDiagnosis, "id", diagnosisId);
            inProgressDiagnosis.startDiagnosis();

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(inProgressDiagnosis));

            // When & Then
            assertThatThrownBy(() -> diagnosisService.findDiagnosisResult(diagnosisId, memberId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("완료되지 않은 진단입니다");

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }
    }

    @Nested
    @DisplayName("updateDiagnosisStatus 메서드 테스트")
    class UpdateDiagnosisStatusTests {

        @Test
        @DisplayName("성공: PENDING → IN_PROGRESS 상태 전환")
        void updateDiagnosisStatus_Success_PendingToInProgress() {
            // Given
            Long diagnosisId = 100L;
            DiagnosisResult pendingDiagnosis = DiagnosisResult.createPending(1L);
            ReflectionTestUtils.setField(pendingDiagnosis, "id", diagnosisId);

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(pendingDiagnosis));
            when(diagnosisResultRepository.save(any(DiagnosisResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS);

            // Then
            assertThat(pendingDiagnosis.getStatus()).isEqualTo(DiagnosisStatus.IN_PROGRESS);
            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
            verify(diagnosisResultRepository, times(1)).save(pendingDiagnosis);
        }

        @Test
        @DisplayName("성공: AWAITING_USER_INPUT → IN_PROGRESS 상태 전환 (resume)")
        void updateDiagnosisStatus_Success_AwaitingToInProgress() {
            // Given
            Long diagnosisId = 100L;
            DiagnosisResult awaitingDiagnosis = DiagnosisResult.createPending(1L);
            ReflectionTestUtils.setField(awaitingDiagnosis, "id", diagnosisId);
            awaitingDiagnosis.startDiagnosis();
            awaitingDiagnosis.awaitUserInput();

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(awaitingDiagnosis));
            when(diagnosisResultRepository.save(any(DiagnosisResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS);

            // Then
            assertThat(awaitingDiagnosis.getStatus()).isEqualTo(DiagnosisStatus.IN_PROGRESS);
            verify(diagnosisResultRepository, times(1)).save(awaitingDiagnosis);
        }

        @Test
        @DisplayName("성공: IN_PROGRESS → AWAITING_USER_INPUT 상태 전환")
        void updateDiagnosisStatus_Success_InProgressToAwaiting() {
            // Given
            Long diagnosisId = 100L;
            DiagnosisResult inProgressDiagnosis = DiagnosisResult.createPending(1L);
            ReflectionTestUtils.setField(inProgressDiagnosis, "id", diagnosisId);
            inProgressDiagnosis.startDiagnosis();

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(inProgressDiagnosis));
            when(diagnosisResultRepository.save(any(DiagnosisResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.AWAITING_USER_INPUT);

            // Then
            assertThat(inProgressDiagnosis.getStatus()).isEqualTo(DiagnosisStatus.AWAITING_USER_INPUT);
            verify(diagnosisResultRepository, times(1)).save(inProgressDiagnosis);
        }

        @Test
        @DisplayName("성공: 수동으로 FAILED 상태로 전환")
        void updateDiagnosisStatus_Success_ManualFailed() {
            // Given
            Long diagnosisId = 100L;
            DiagnosisResult inProgressDiagnosis = DiagnosisResult.createPending(1L);
            ReflectionTestUtils.setField(inProgressDiagnosis, "id", diagnosisId);
            inProgressDiagnosis.startDiagnosis();

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(inProgressDiagnosis));
            when(diagnosisResultRepository.save(any(DiagnosisResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.FAILED);

            // Then
            assertThat(inProgressDiagnosis.getStatus()).isEqualTo(DiagnosisStatus.FAILED);
            verify(diagnosisResultRepository, times(1)).save(inProgressDiagnosis);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 진단 ID로 상태 업데이트 시 예외 발생")
        void updateDiagnosisStatus_ThrowsException_WhenDiagnosisNotFound() {
            // Given
            Long diagnosisId = 999L;

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("진단 정보를 찾을 수 없습니다");

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
            verify(diagnosisResultRepository, never()).save(any(DiagnosisResult.class));
        }
    }

    @Nested
    @DisplayName("verifyDiagnosisOwnershipById 메서드 테스트")
    class VerifyDiagnosisOwnershipTests {

        @Test
        @DisplayName("성공: 소유자가 올바를 때 검증 성공")
        void verifyDiagnosisOwnership_Success_WhenOwnerMatches() {
            // Given
            Long diagnosisId = 100L;
            Long memberId = 1L;

            DiagnosisResult diagnosis = DiagnosisResult.createPending(memberId);
            ReflectionTestUtils.setField(diagnosis, "id", diagnosisId);

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(diagnosis));

            // When & Then
            assertThatCode(() -> diagnosisService.verifyDiagnosisOwnershipById(diagnosisId, memberId))
                    .doesNotThrowAnyException();

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }

        @Test
        @DisplayName("실패: 소유자가 다를 때 예외 발생")
        void verifyDiagnosisOwnership_ThrowsException_WhenOwnerDifferent() {
            // Given
            Long diagnosisId = 100L;
            Long ownerId = 1L;
            Long otherUserId = 2L;

            DiagnosisResult diagnosis = DiagnosisResult.createPending(ownerId);
            ReflectionTestUtils.setField(diagnosis, "id", diagnosisId);

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.of(diagnosis));

            // When & Then
            assertThatThrownBy(() -> diagnosisService.verifyDiagnosisOwnershipById(diagnosisId, otherUserId))
                    .isInstanceOf(DiagnosisAccessDeniedException.class);

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 진단 ID로 검증 시 예외 발생")
        void verifyDiagnosisOwnership_ThrowsException_WhenDiagnosisNotFound() {
            // Given
            Long diagnosisId = 999L;
            Long memberId = 1L;

            when(diagnosisResultRepository.findById(diagnosisId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> diagnosisService.verifyDiagnosisOwnershipById(diagnosisId, memberId))
                    .isInstanceOf(DiagnosisNotFoundException.class);

            verify(diagnosisResultRepository, times(1)).findById(diagnosisId);
        }
    }
}

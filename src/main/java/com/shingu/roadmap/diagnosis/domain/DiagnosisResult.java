package com.shingu.roadmap.diagnosis.domain;

import com.shingu.roadmap.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * 진단 결과 Aggregate Root
 *
 * 역할:
 * - 사용자의 역량 진단 결과를 관리하는 독립적인 Aggregate Root
 * - Member와는 ID 참조를 통해 느슨하게 결합 (DDD Cross-Aggregate Reference Pattern)
 * - 진단 데이터의 일관성과 무결성을 보장
 *
 * 설계 특징:
 * - Member 엔티티와 분리된 독립적인 Aggregate (별도의 트랜잭션 경계)
 * - 복잡한 진단 데이터는 DiagnosisResultData Value Object로 캡슐화
 * - JPA Converter를 통한 타입 안전 JSON 저장
 * - 상태 전이를 제어하는 비즈니스 메서드 제공
 *
 * 관계:
 * - Member (N:1): memberId로 참조 (객체 참조 아님, DDD 원칙)
 */
@Entity
@Table(
        name = "diagnosis_results",
        indexes = {
                @Index(name = "idx_diagnosis_member_id", columnList = "member_id"),
                @Index(name = "idx_diagnosis_status", columnList = "status"),
                @Index(name = "idx_diagnosis_member_status", columnList = "member_id, status"),
                @Index(name = "idx_diagnosis_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class DiagnosisResult extends BaseEntity {

    /**
     * 진단 대상 회원 ID
     *
     * 설계 노트:
     * - @ManyToOne 대신 Long 타입 사용 (DDD Cross-Aggregate Reference Pattern)
     * - Member Aggregate와 느슨한 결합 유지
     * - 외래키 제약조건은 데이터베이스 레벨에서 관리
     * - 쿼리 성능 향상 (불필요한 Join 방지)
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * 진단 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiagnosisStatus status;

    /**
     * 진단 결과 데이터 (JSON으로 저장)
     *
     * 설계 노트:
     * - DiagnosisResultData Value Object를 JSON 컬럼에 저장
     * - JPA AttributeConverter가 자동으로 변환 처리
     * - 타입 안전성 보장 (String이 아닌 객체로 관리)
     * - MariaDB JSON 타입 활용 (10.2+)
     */
    @Convert(converter = DiagnosisResultDataConverter.class)
    @Column(name = "result_data", columnDefinition = "JSON")
    private DiagnosisResultData resultData;

    /**
     * 진단 결과 생성 (초기 상태)
     *
     * @param memberId 회원 ID
     * @return 생성된 진단 결과
     */
    public static DiagnosisResult createPending(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        return DiagnosisResult.builder()
                .memberId(memberId)
                .status(DiagnosisStatus.PENDING)
                .resultData(null)
                .build();
    }

    /**
     * 진단 시작 (상태를 IN_PROGRESS로 변경)
     */
    public void startDiagnosis() {
        if (this.status != DiagnosisStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("PENDING 상태에서만 진단을 시작할 수 있습니다. 현재 상태: %s", this.status)
            );
        }
        this.status = DiagnosisStatus.IN_PROGRESS;
    }

    /**
     * 진단 완료 (결과 데이터 저장)
     *
     * @param resultData 진단 결과 데이터
     */
    public void completeDiagnosis(DiagnosisResultData resultData) {
        if (resultData == null) {
            throw new IllegalArgumentException("진단 결과 데이터는 필수입니다.");
        }

        if (this.status == DiagnosisStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 진단입니다.");
        }

        this.status = DiagnosisStatus.COMPLETED;
        this.resultData = resultData;
    }

    /**
     * 진단 실패 처리
     *
     * @param errorMessage 실패 사유 (로깅용, 저장하지 않음)
     */
    public void failDiagnosis(String errorMessage) {
        if (this.status == DiagnosisStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 진단은 실패 처리할 수 없습니다.");
        }

        this.status = DiagnosisStatus.FAILED;
        // 실패 시에는 resultData를 null로 유지
    }

    /**
     * 사용자 입력 대기 상태로 변경 (Human-in-the-loop)
     */
    public void awaitUserInput() {
        if (this.status != DiagnosisStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    String.format("IN_PROGRESS 상태에서만 사용자 입력 대기로 변경할 수 있습니다. 현재 상태: %s", this.status)
            );
        }
        this.status = DiagnosisStatus.AWAITING_USER_INPUT;
    }

    /**
     * 사용자 입력 후 진단 재개
     */
    public void resumeDiagnosis() {
        if (this.status != DiagnosisStatus.AWAITING_USER_INPUT) {
            throw new IllegalStateException(
                    String.format("AWAITING_USER_INPUT 상태에서만 진단을 재개할 수 있습니다. 현재 상태: %s", this.status)
            );
        }
        this.status = DiagnosisStatus.IN_PROGRESS;
    }

    /**
     * 진단 결과 업데이트 (기존 완료된 진단 수정)
     *
     * @param resultData 새로운 진단 결과 데이터
     */
    public void updateResult(DiagnosisResultData resultData) {
        if (resultData == null) {
            throw new IllegalArgumentException("진단 결과 데이터는 필수입니다.");
        }

        if (this.status != DiagnosisStatus.COMPLETED) {
            throw new IllegalStateException(
                    String.format("완료된 진단만 수정할 수 있습니다. 현재 상태: %s", this.status)
            );
        }

        this.resultData = resultData;
    }

    /**
     * 진단이 완료되었는지 확인
     *
     * @return 완료 여부
     */
    public boolean isCompleted() {
        return this.status == DiagnosisStatus.COMPLETED;
    }

    /**
     * 진단이 진행 중인지 확인
     *
     * @return 진행 중 여부
     */
    public boolean isInProgress() {
        return this.status == DiagnosisStatus.IN_PROGRESS;
    }

    /**
     * 진단이 실패했는지 확인
     *
     * @return 실패 여부
     */
    public boolean isFailed() {
        return this.status == DiagnosisStatus.FAILED;
    }

    /**
     * 신뢰도 점수 조회 (resultData에서 추출)
     *
     * @return 신뢰도 점수 (없으면 null)
     */
    public Double getConfidenceScore() {
        if (resultData == null) {
            return null;
        }
        return resultData.getConfidenceScore();
    }

    /**
     * 요약 조회 (resultData에서 추출)
     *
     * @return 요약 (없으면 null)
     */
    public String getSummary() {
        return resultData != null ? resultData.getSummary() : null;
    }
}

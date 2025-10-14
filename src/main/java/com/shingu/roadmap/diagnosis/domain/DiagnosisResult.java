package com.shingu.roadmap.diagnosis.domain;

import com.shingu.roadmap.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 진단 결과 엔티티
 * 사용자의 역량 진단 결과를 저장합니다.
 */
@Entity
@Table(name = "diagnosis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class DiagnosisResult extends BaseEntity {

    /**
     * 진단 대상 회원 ID
     */
    @Column(nullable = false)
    private Long memberId;

    /**
     * 진단 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiagnosisStatus status;

    /**
     * 진단 결과 요약
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * 전체 진단 결과 JSON
     * DiagnosisResultResponse를 JSON으로 직렬화하여 저장
     */
    @Column(columnDefinition = "JSON", nullable = false)
    private String resultJson;

    /**
     * 전체 분석에 대한 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column
    private Double confidenceScore;

    /**
     * 진단 결과 업데이트
     *
     * @param status 진단 상태
     * @param resultJson 결과 JSON
     * @param confidenceScore 신뢰도 점수
     * @param summary 요약
     */
    public void updateResult(DiagnosisStatus status, String resultJson, Double confidenceScore, String summary) {
        this.status = status;
        this.resultJson = resultJson;
        this.confidenceScore = confidenceScore;
        this.summary = summary;
    }
}

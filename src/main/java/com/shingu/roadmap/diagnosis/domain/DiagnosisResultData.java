package com.shingu.roadmap.diagnosis.domain;

import com.shingu.roadmap.diagnosis.dto.common.RadarChartData;
import com.shingu.roadmap.diagnosis.dto.response.CertificationRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.JobRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import lombok.*;

import java.util.List;

/**
 * 진단 결과 데이터 Value Object
 * DiagnosisResultResponse의 핵심 데이터를 JSON으로 변환하여 저장하기 위한 불변 객체
 *
 * 설계 특징:
 * - 불변 객체로 설계 (Value Object 패턴)
 * - JPA Converter를 통해 JSON 컬럼으로 변환
 * - DiagnosisResultResponse와 독립적으로 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode
public class DiagnosisResultData {

    /**
     * 진단 결과 요약
     */
    private String summary;

    /**
     * 추천 NCS 코드 목록 (각 후보별 KSA 분석 포함)
     */
    private List<NcsAnalysisResponse> ncsAnalyses;

    /**
     * 전체 분석에 대한 신뢰도 점수 (0.0 ~ 1.0)
     */
    private Double confidenceScore;

    /**
     * 레이더 차트 데이터 (사용자 역량 vs 여러 목표 NCS 직무 동시 비교)
     */
    private RadarChartData radarChartData;

    /**
     * 추천 채용공고 목록
     */
    private List<JobRecommendationResponse> jobRecommendations;

    /**
     * 추천 자격증 목록
     */
    private List<CertificationRecommendationResponse> certificationRecommendations;

    /**
     * DiagnosisResultResponse로부터 DiagnosisResultData 생성
     *
     * @param summary 진단 결과 요약
     * @param ncsAnalyses NCS 분석 결과 목록
     * @param confidenceScore 신뢰도 점수
     * @param radarChartData 레이더 차트 데이터
     * @param jobRecommendations 추천 채용공고 목록
     * @param certificationRecommendations 추천 자격증 목록
     * @return DiagnosisResultData 객체
     */
    public static DiagnosisResultData fromResponse(
            String summary,
            List<NcsAnalysisResponse> ncsAnalyses,
            Double confidenceScore,
            RadarChartData radarChartData,
            List<JobRecommendationResponse> jobRecommendations,
            List<CertificationRecommendationResponse> certificationRecommendations
    ) {
        return DiagnosisResultData.builder()
                .summary(summary)
                .ncsAnalyses(ncsAnalyses)
                .confidenceScore(confidenceScore)
                .radarChartData(radarChartData)
                .jobRecommendations(jobRecommendations)
                .certificationRecommendations(certificationRecommendations)
                .build();
    }
}
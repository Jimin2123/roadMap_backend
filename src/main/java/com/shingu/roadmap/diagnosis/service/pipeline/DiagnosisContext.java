package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.diagnosis.dto.response.CertificationRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.dto.response.JobRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.member.domain.Profile;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

/**
 * 진단 파이프라인을 통해 전달되는 컨텍스트 객체
 * 각 프로세서는 이 컨텍스트를 읽고 수정하여 다음 단계로 전달합니다.
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class DiagnosisContext {

    /**
     * 진단 대상 사용자 ID
     */
    private Long memberId;

    /**
     * 진단 ID (SSE 스트리밍을 위한 식별자)
     */
    private Long diagnosisId;

    /**
     * 사용자 프로필 (이력서, 스킬, 자격증 등 포함)
     */
    private Profile profile;

    /**
     * 1단계: NCS 직무 추천 및 검증 결과
     */
    private NcsAnalysisResponse ncsAnalysisResponse;

    /**
     * 2단계: KSA 역량 분석 결과 목록 (여러 NCS 코드에 대해 분석 가능)
     */
    private List<KsaAnalysisResponse> ksaAnalysisResponses;

    /**
     * 2단계: 커리어 레벨 진단 결과
     */
    private String careerLevel;

    /**
     * 3단계: 채용공고 추천 결과
     */
    private List<JobRecommendationResponse> jobRecommendations;

    /**
     * 3단계: 자격증 추천 결과
     */
    private List<CertificationRecommendationResponse> certificationRecommendations;

    /**
     * 4단계: 최종 진단 리포트
     */
    private DiagnosisResultResponse diagnosisResultResponse;

    /**
     * 사용자가 직접 선택한 NCS 코드 (신뢰도가 낮을 경우)
     */
    private String userSelectedNcsCode;

    /**
     * 에러 메시지 (처리 중 발생한 오류)
     */
    private String errorMessage;

    /**
     * 진단 성공 여부
     */
    private boolean success;

    /**
     * 진행 상황 알림 콜백 (SSE 전송용)
     */
    private Consumer<DiagnosisProgressResponse> progressCallback;
}

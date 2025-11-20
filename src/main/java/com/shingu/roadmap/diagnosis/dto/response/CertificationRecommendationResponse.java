package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 자격증 추천 응답 DTO
 * 사용자에게 추천된 개별 자격증 정보
 */
@Builder
@Schema(description = "자격증 추천 정보")
public record CertificationRecommendationResponse(

        @Schema(description = "자격증명", example = "정보처리기사")
        String certificationName,

        @Schema(description = "발급 기관", example = "한국산업인력공단")
        String issuingOrganization,

        @Schema(description = "자격증 분야", example = "정보기술")
        String category,

        @Schema(description = "난이도 (1-5)", example = "3")
        Integer difficultyLevel,

        @Schema(description = "추천 우선순위 (1-5, 낮을수록 높은 우선순위)", example = "1")
        Integer priority,

        @Schema(description = "추천 이유", example = "백엔드 개발자 직무에 필수적인 정보처리 관련 자격증으로, 귀하의 현재 역량을 공식적으로 인증할 수 있습니다.")
        String reason,

        @Schema(description = "현재 보유 여부", example = "false")
        Boolean isOwned,

        @Schema(description = "역량 gap 해소 기여도 (%)", example = "35")
        Integer gapResolutionContribution,

        @Schema(description = "관련 NCS 코드", example = "20010202")
        String relatedNcsCode,

        @Schema(description = "추정 준비 기간 (개월)", example = "3")
        Integer estimatedPreparationMonths

) {
}

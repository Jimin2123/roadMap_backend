package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 채용공고 추천 응답 DTO
 * 사용자에게 추천된 개별 채용공고 정보
 */
@Builder
@Schema(description = "채용공고 추천 정보")
public record JobRecommendationResponse(

        @Schema(description = "채용공고 ID", example = "12345678")
        String jobId,

        @Schema(description = "채용공고 제목", example = "백엔드 개발자 (Spring Boot)")
        String title,

        @Schema(description = "회사명", example = "신구대학교")
        String companyName,

        @Schema(description = "회사 로고 URL", example = "https://example.com/logo.png")
        String companyLogoUrl,

        @Schema(description = "채용공고 상세 URL", example = "https://www.saramin.co.kr/zf_user/jobs/relay/view?rec_idx=12345678")
        String url,

        @Schema(description = "지역 정보", example = "서울 > 강남구")
        String location,

        @Schema(description = "경력 요구사항", example = "신입·경력")
        String experienceLevel,

        @Schema(description = "학력 요구사항", example = "대졸↑")
        String educationLevel,

        @Schema(description = "직무 코드", example = "2")
        String jobCode,

        @Schema(description = "직무명", example = "백엔드/서버개발")
        String jobName,

        @Schema(description = "급여 정보", example = "회사내규에 따름")
        String salary,

        @Schema(description = "마감일 (timestamp)", example = "1640966400")
        String expirationTimestamp,

        @Schema(description = "추천 이유 (AI 생성)", example = "귀하의 Spring Boot 및 MariaDB 경험과 잘 맞는 백엔드 개발 포지션입니다.")
        String recommendationReason,

        @Schema(description = "매칭 점수 (0-100)", example = "85")
        Integer matchScore

) {
}

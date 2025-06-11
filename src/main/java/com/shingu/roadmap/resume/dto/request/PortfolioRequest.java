package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포트폴리오 등록 요청 DTO")
public record PortfolioRequest(
        @Schema(description = "포트폴리오 제목", example = "개인 프로젝트: 쇼핑몰 웹사이트")
        String title,

        @Schema(description = "포트폴리오 URL", example = "http://example.com/portfolio/shopping-mall")
        String url
) {
}

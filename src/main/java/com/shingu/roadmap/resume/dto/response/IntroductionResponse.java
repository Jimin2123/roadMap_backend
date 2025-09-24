package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Introduction;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자기소개 응답 DTO")
public record IntroductionResponse(
        @Schema(description = "자기소개 ID")
        Long id,

        @Schema(description = "자기소개 내용")
        String content
) {
    public static IntroductionResponse from(Introduction introduction) {
        if (introduction == null) return null;

        return new IntroductionResponse(
                introduction.getId(),
                introduction.getContent()
        );
    }
}
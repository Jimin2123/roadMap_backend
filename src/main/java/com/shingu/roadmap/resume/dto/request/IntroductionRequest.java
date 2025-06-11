package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자기소개 등록 요청 DTO")
public record IntroductionRequest(
        @Schema(description = "자기소개 내용")
        String content
) { }

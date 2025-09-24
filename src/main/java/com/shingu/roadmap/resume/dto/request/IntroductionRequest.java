package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "자기소개 등록 요청 DTO")
public record IntroductionRequest(
        @Schema(description = "자기소개 내용")
        @Size(max = 3000, message = "자기소개는 3000자를 초과할 수 없습니다.")
        String content
) { }

package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "자기소개 등록/수정 요청 DTO")
public record IntroductionRequest(
        @Schema(description = "성장과정")
        @Size(max = 3000, message = "성장과정은 3000자를 초과할 수 없습니다.")
        String growthProcess,

        @Schema(description = "장점 및 강점")
        @Size(max = 3000, message = "장점 및 강점은 3000자를 초과할 수 없습니다.")
        String strengths,

        @Schema(description = "학교생활")
        @Size(max = 3000, message = "학교생활은 3000자를 초과할 수 없습니다.")
        String schoolLife,

        @Schema(description = "지원동기")
        @Size(max = 3000, message = "지원동기는 3000자를 초과할 수 없습니다.")
        String motivation
) {
}

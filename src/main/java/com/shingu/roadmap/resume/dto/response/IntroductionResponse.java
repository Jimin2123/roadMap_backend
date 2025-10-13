package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Introduction;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자기소개 응답 DTO")
public record IntroductionResponse(
        @Schema(description = "자기소개 ID")
        Long id,

        @Schema(description = "성장과정")
        String growthProcess,

        @Schema(description = "장점 및 강점")
        String strengths,

        @Schema(description = "학교생활")
        String schoolLife,

        @Schema(description = "지원동기")
        String motivation
) {
  public static IntroductionResponse from(Introduction introduction) {
    if (introduction == null) {
      return null;
    }

    return new IntroductionResponse(
            introduction.getId(),
            introduction.getGrowthProcess(),
            introduction.getStrengths(),
            introduction.getSchoolLife(),
            introduction.getMotivation()
    );
  }
}
package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "NCS 코드 추천 후보")
public record NcsRecommendationCandidate(
        @Schema(description = "NCS 코드", example = "02010201")
        String ncsCode,

        @Schema(description = "NCS 직무명", example = "소프트웨어 아키텍트")
        String ncsName,

        @Schema(description = "AI가 판단한 해당 직무와의 적합도 점수 (0.0 ~ 1.0)", example = "0.91")
        Double confidenceScore,

        @Schema(description = "해당 직무를 추천한 이유")
        String reason,

        @Schema(description = "추천 이유에 대한 근거 데이터")
        List<Evidence> reasonEvidence
) {
  @Schema(description = "근거 데이터")
  public record Evidence(
          @Schema(description = "근거 내용", example = "React 프로젝트 3개 이상 경험")
          String content,

          @Schema(description = "근거 데이터의 출처")
          Source source
  ) {
  }

  @Schema(description = "근거 데이터 출처")
  public record Source(
          @Schema(description = "출처 유형", example = "PROJECT",
                  allowableValues = {"PROJECT", "EDUCATION", "CERTIFICATION", "EXPERIENCE", "SKILL"})
          String type,

          @Schema(description = "출처 식별자", example = "project-456")
          String id,

          @Schema(description = "출처 제목", example = "전자상거래 플랫폼 개발")
          String title
  ) {
  }
}

package com.shingu.roadmap.diagnosis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 직무 선택 확인 요청 DTO (Human-in-the-loop)")
public record JobConfirmationRequest(
        @NotBlank
        @Schema(description = "사용자가 선택한 NCS 직무 코드", example = "02010201")
        String selectedNcsCode,

        @NotBlank
        @Schema(description = "사용자가 선택한 직무명", example = "소프트웨어 아키텍트")
        String selectedJobName
) {
}
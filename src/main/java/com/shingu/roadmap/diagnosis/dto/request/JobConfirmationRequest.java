package com.shingu.roadmap.diagnosis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 직무 선택 확인 요청 DTO (Human-in-the-loop)")
public record JobConfirmationRequest(
        @NotBlank(message = "NCS 직무 코드는 필수입니다")
        @Pattern(regexp = "^\\d{8}$", message = "NCS 코드는 8자리 숫자여야 합니다")
        @Schema(
                description = "사용자가 선택한 NCS 직무 코드",
                example = "02010201",
                pattern = "^\\d{8}$",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String selectedNcsCode,

        @NotBlank(message = "직무명은 필수입니다")
        @Size(max = 100, message = "직무명은 최대 100자까지 입력 가능합니다")
        @Schema(
                description = "사용자가 선택한 직무명",
                example = "소프트웨어 아키텍트",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String selectedJobName
) {
}
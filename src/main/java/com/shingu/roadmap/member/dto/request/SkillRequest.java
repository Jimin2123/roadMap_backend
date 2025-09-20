package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.common.enums.SkillProficiency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "보유 기술 등록 요청 DTO")
public record SkillRequest(
        @Schema(description = "기술 이름", example = "Java")
        @NotBlank(message = "기술 이름은 필수입니다")
        @Size(max = 50, message = "기술 이름은 50자를 초과할 수 없습니다")
        String name,

        @Schema(description = "숙련도", example = "ADVANCED", implementation = SkillProficiency.class)
        @NotNull(message = "숙련도는 필수입니다")
        SkillProficiency proficiency
) {
}
package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.common.enums.SkillProficiency;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보유 기술 등록 요청 DTO")
public record SkillRequest(
        @Schema(description = "기술 이름", example = "Java")
        String name,

        @Schema(description = "숙련도", example = "ADVANCED", implementation = SkillProficiency.class)
        SkillProficiency proficiency
) {
}
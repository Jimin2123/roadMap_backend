package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.common.enums.SkillProficiency;
import com.shingu.roadmap.member.domain.ProfileSkill;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "보유 기술 응답 DTO")
@Builder
public record ProfileSkillDTO(
        @Schema(description = "기술 ID", example = "1")
        Long id,
        @Schema(description = "기술 이름", example = "Spring Boot")
        String name,
        @Schema(description = "숙련도", example = "INTERMEDIATE")
        SkillProficiency proficiency
) {
  public static ProfileSkillDTO from(ProfileSkill profileSkill) {
    if (profileSkill == null) return null;

    return ProfileSkillDTO.builder()
            .id(profileSkill.getSkill().getId())
            .name(profileSkill.getSkill().getName())
            .proficiency(profileSkill.getProficiency())
            .build();
  }
}
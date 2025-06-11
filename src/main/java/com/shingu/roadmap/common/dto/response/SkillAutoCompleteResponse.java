package com.shingu.roadmap.common.dto.response;

import com.shingu.roadmap.common.domain.Skill;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기술 목록 응답 DTO")
public record SkillAutoCompleteResponse(
        @Schema(description = "기술 아이디", example = "123456")
        Long id,

        @Schema(description = "기술 이름", example = "Java")
        String name
) {
        public static SkillAutoCompleteResponse from(Skill s) {
                return new SkillAutoCompleteResponse(
                        s.getId(),
                        s.getName()
                );
        }
}

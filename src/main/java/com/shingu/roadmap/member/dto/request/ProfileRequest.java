package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.common.enums.EducationLevelType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "회원 프로필 등록 요청 DTO")
public record ProfileRequest(

        @Schema(description = "희망 직무", example = "백엔드 개발자")
        String desiredJob,

        @Schema(description = "전공", example = "컴퓨터공학")
        String major,

        @Schema(description = "학력", example = "UNIVERSITY")
        EducationLevelType educationLevel,

        @Schema(description = "보유 기술", example = "[\"Java\", \"Spring\"]")
        Set<String> skills,

        @Schema(description = "자격증", example = "[\"정보처리기사\", \"JLPT 2급\"]")
        Set<String> certificates
) {}

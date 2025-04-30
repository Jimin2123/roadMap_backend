package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "회원 프로필 응답 DTO")
public record ProfileResponse(

        @Schema(description = "학력", example = "College")
        EducationLevelType educationLevel,

        @Schema(description = "희망 직무", example = "백엔드 개발자")
        String desiredJob,

        @Schema(description = "전공", example = "컴퓨터공학")
        String major,

        @Schema(description = "보유 기술 목록", example = "[\"Java\", \"Spring\"]")
        Set<String> skills,

        @Schema(description = "자격증 목록", example = "[\"정보처리기사\", \"JLPT 2급\"]")
        Set<String> certificates,

        @Schema(description = "NCS 코드 목록", example = "[\"NCS_001\", \"NCS_002\"]")
        Set<String> ncsCodes
) {
  public static ProfileResponse from(Member member) {
    Profile p = member.getProfile();
    return new ProfileResponse(
            p.getEducationLevel(),
            p.getDesiredJob(),
            p.getMajor(),
            member.getSkills(),
            member.getCertificates(),
            member.getNcsCodes()
    );
  }
}

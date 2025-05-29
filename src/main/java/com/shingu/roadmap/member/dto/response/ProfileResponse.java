package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.member.domain.Certificate;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.domain.Skill;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.stream.Collectors;

@Schema(description = "회원 프로필 응답 DTO")
public record ProfileResponse(

        @Schema(description = "학력", example = "College")
        String educationLevel,

        @Schema(description = "전공", example = "컴퓨터공학")
        String major,

        @Schema(description = "희망 직무", example = "백엔드 개발자")
        String desiredJob,

        @Schema(description = "자격증 목록", example = "[\"정보처리기사\", \"JLPT 2급\"]")
        Set<Certificate> certificates,

        @Schema(description = "보유 기술 목록", example = "[\"Java\", \"Spring\"]")
        Set<Skill> skills,

        @Schema(description = "희망 직무 NCS 코드 목록", example = "[\"NCS_001\", \"NCS_002\"]")
        Set<NcsOccupation> desiredCapabilities,

        @Schema(description = "사용자 NCS 코드 목록", example = "[\"NCS_001\", \"NCS_002\"]")
        Set<NcsOccupation> userCapabilities
) {
        public static ProfileResponse from(Profile profile) {
                if (profile == null) return null;

                return new ProfileResponse(
                        profile.getEducationLevel(),
                        profile.getMajor(),
                        profile.getDesiredJob(),
                        profile.getCertificates(),
                        profile.getSkills(),
                        profile.getDesiredCapabilities(),
                        profile.getUserCapabilities()
                );
        }
}

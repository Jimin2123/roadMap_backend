package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobDto;
import com.shingu.roadmap.common.domain.Certificate;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.resume.dto.response.ResumeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.stream.Collectors;

@Schema(description = "회원 프로필 응답 DTO")
public record ProfileResponse(

        @Schema(description = "학력", example = "College")
        String educationLevel,

        @Schema(description = "희망 직무 목록")
        Set<SaraminJobDto> desiredJob,

        @Schema(description = "자격증 목록")
        Set<Certificate> certificates,

        @Schema(description = "보유 기술 목록")
        Set<Skill> skills,

        @Schema(description = "희망 직무 NCS 코드 목록")
        Set<NcsOccupation> desiredCapabilities,

        @Schema(description = "사용자 NCS 코드 목록")
        Set<NcsOccupation> userCapabilities,

        @Schema(description = "이력서 정보")
        ResumeResponse resume
) {
        public static ProfileResponse from(Profile profile) {
                if (profile == null) return null;

                return new ProfileResponse(
                        profile.getEducationLevel(),
                        profile.getDesiredJobs().stream().map(SaraminJobDto::from).collect(Collectors.toSet()),
                        profile.getCertificates(),
                        profile.getSkills(),
                        profile.getDesiredCapabilities(),
                        profile.getUserCapabilities(),
                        ResumeResponse.from(profile.getResume())
                );
        }
}
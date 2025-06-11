package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.resume.dto.request.ResumeRequest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "회원 프로필 등록 요청 DTO")
public record ProfileRequest(

        @Schema(description = "희망 직무 코드", example = "[84, 85]" )
        Set<Integer> desiredJobCodes,

        @Schema(description = "학력", example = "ASSOCIATE_DEGREE", implementation = EducationLevelType.class)
        EducationLevelType educationLevel,

        @Schema(description = "보유 기술", example = "[\"Java\", \"Spring\"]")
        Set<String> skills,

        @Schema(description = "자격증", example = "[\"정보처리기사\", \"JLPT 2급\"]")
        Set<String> certificates,

        @Schema(description = "이력서 정보", implementation = ResumeRequest.class)
        ResumeRequest resume
) { }

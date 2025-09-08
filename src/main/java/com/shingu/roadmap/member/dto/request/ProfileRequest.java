package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.common.dto.CertificateDTO;
import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.resume.dto.request.ResumeRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "회원 프로필 등록 요청 DTO")
public record ProfileRequest(

        @Schema(description = "희망 직무 코드", example = "[84, 85]" )
        Set<Integer> desiredJobCodes,

        @Schema(description = "현재 직업", example = "학생")
        String currentJob,

        @Schema(description = "학력", example = "ASSOCIATE_DEGREE", implementation = EducationLevelType.class)
        EducationLevelType educationLevel,

        // 👇 이 부분이 변경되었습니다.
        @ArraySchema(schema = @Schema(description = "보유 기술 목록", implementation = SkillRequest.class))
        Set<SkillRequest> skills,

        @ArraySchema(schema = @Schema(description = "자격증", implementation = CertificateDTO.class))
        Set<CertificateDTO> certificates,

        @Schema(description = "이력서 정보", implementation = ResumeRequest.class)
        ResumeRequest resume
) { }

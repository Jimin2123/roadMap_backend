package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.common.dto.CertificateDTO;
import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.resume.dto.request.ResumeRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "회원 프로필 등록 요청 DTO")
public record ProfileRequest(

        @Schema(description = "희망 직무 코드", example = "[84, 85]" )
        @Size(max = 10, message = "희망 직무는 최대 10개까지 선택 가능합니다")
        Set<Integer> desiredJobCodes,

        @Schema(description = "현재 직업", example = "학생")
        @Size(max = 100, message = "현재 직업은 100자를 초과할 수 없습니다")
        String currentJob,

        @Schema(description = "학력", example = "ASSOCIATE_DEGREE", implementation = EducationLevelType.class)
        EducationLevelType educationLevel,

        @ArraySchema(schema = @Schema(description = "보유 기술 목록", implementation = SkillRequest.class))
        @Size(max = 50, message = "기술은 최대 50개까지 등록 가능합니다")
        @Valid
        Set<SkillRequest> skills,

        @ArraySchema(schema = @Schema(description = "자격증", implementation = CertificateDTO.class))
        @Size(max = 30, message = "자격증은 최대 30개까지 등록 가능합니다")
        @Valid
        Set<CertificateDTO> certificates,

        @Schema(description = "이력서 정보", implementation = ResumeRequest.class)
        @Valid
        ResumeRequest resume
) { }

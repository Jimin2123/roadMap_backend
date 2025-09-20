package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "이력서 등록 요청 DTO")
public record ResumeRequest(

        @Schema(description = "자기소개" + " 요청 DTO", implementation = IntroductionRequest.class)
        @Valid
        IntroductionRequest introduction,

        @ArraySchema(schema = @Schema(description = "활동내역 목록", implementation = ActivityRequest.class))
        @Size(max = 20, message = "활동내역은 최대 20개까지 등록 가능합니다")
        @Valid
        List<ActivityRequest> activities,

        @ArraySchema(schema = @Schema(description = "프로젝트 목록", implementation = ProjectRequest.class))
        @Size(max = 15, message = "프로젝트는 최대 15개까지 등록 가능합니다")
        @Valid
        List<ProjectRequest> projects,

        @Schema(description = "학력 정보 목록", implementation = EducationRequest.class)
        @Valid
        EducationRequest education
) { }

package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "이력서 등록 요청 DTO")
public record ResumeRequest(

        @Schema(description = "자기소개" + " 요청 DTO", implementation = IntroductionRequest.class)
        IntroductionRequest introduction,

        @ArraySchema(schema = @Schema(description = "활동내역 목록", implementation = ActivityRequest.class))
        List<ActivityRequest> activities,

        @ArraySchema(schema = @Schema(description = "포트폴리오 목록", implementation = PortfolioRequest.class))
        List<PortfolioRequest> portfolios,

        @ArraySchema(schema = @Schema(description = "프로젝트 목록", implementation = ProjectRequest.class))
        List<ProjectRequest> projects,

        @Schema(description = "학력 정보 목록", implementation = EducationRequest.class)
        EducationRequest education
) { }

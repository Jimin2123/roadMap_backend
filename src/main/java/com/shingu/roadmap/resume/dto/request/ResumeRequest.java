package com.shingu.roadmap.resume.dto.request;

import com.shingu.roadmap.common.dto.CertificateDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

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

        @ArraySchema(schema = @Schema(description = "경력사항 목록", implementation = CareerRequest.class))
        @Size(max = 20, message = "경력사항은 최대 20개까지 등록 가능합니다")
        @Valid
        List<CareerRequest> careers,

        @Schema(description = "학력 정보 목록", implementation = EducationRequest.class)
        @Valid
        EducationRequest education,

        @Schema(description = "희망 직무 및 회사 정보", implementation = DesiredCompanyRequest.class)
        @Valid
        DesiredCompanyRequest desiredCompany,

        @ArraySchema(schema = @Schema(description = "자격증", implementation = CertificateDTO.class))
        @Size(max = 30, message = "자격증은 최대 30개까지 등록 가능합니다")
        @Valid
        Set<CertificateDTO> certificates
) { }

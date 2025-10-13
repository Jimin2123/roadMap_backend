package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "경력사항 등록 요청 DTO")
public record CareerRequest(
    @Schema(description = "회사명", example = "삼성전자")
    @NotBlank(message = "회사명은 필수입니다.")
    @Size(max = 100, message = "회사명은 100자를 초과할 수 없습니다.")
    String companyName,

    @Schema(description = "근무기간", example = "{\"startDate\":\"2020-01-01\",\"endDate\":\"2023-12-31\"}")
    @Valid
    PeriodRequest period,

    @Schema(description = "근무부서", example = "소프트웨어개발팀")
    @Size(max = 100, message = "근무부서는 100자를 초과할 수 없습니다.")
    String department,

    @Schema(description = "업무내용", example = "Spring Boot 기반 백엔드 API 개발 및 유지보수")
    @Size(max = 1000, message = "업무내용은 1000자를 초과할 수 없습니다.")
    String description
) { }

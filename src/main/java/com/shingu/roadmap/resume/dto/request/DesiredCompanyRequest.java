package com.shingu.roadmap.resume.dto.request;

import com.shingu.roadmap.common.enums.SalaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "희망 직무 및 회사 등록 요청 DTO")
public record DesiredCompanyRequest(
        @Schema(description = "희망 회사 1", example = "네이버")
        @Size(max = 100, message = "희망 회사 1은 100자를 초과할 수 없습니다.")
        String desiredCompany1,

        @Schema(description = "희망 회사 2", example = "카카오")
        @Size(max = 100, message = "희망 회사 2는 100자를 초과할 수 없습니다.")
        String desiredCompany2,

        @Schema(description = "희망 지역", example = "서울")
        @Size(max = 100, message = "희망 지역은 100자를 초과할 수 없습니다.")
        String desiredRegion,

        @Schema(description = "희망 급여 형태", example = "연봉")
        SalaryType salaryType,

        @Schema(description = "희망 급여 금액", example = "5000")
        int desiredSalary,

        @Schema(description = "진로 계획", example = "백엔드 개발자로 성장하고 싶습니다.")
        @Size(max = 1000, message = "진로 계획은 1000자를 초과할 수 없습니다.")
        String careerPlan
) {
}

package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.common.enums.SalaryType;
import com.shingu.roadmap.resume.domain.DesiredCompany;

public record DesiredCompanyResponse(
        Long id,
        String desiredCompany1,
        String desiredCompany2,
        String desiredRegion,
        SalaryType salaryType,
        int desiredSalary,
        String careerPlan
) {
    public static DesiredCompanyResponse from(DesiredCompany desiredCompany) {
        if (desiredCompany == null) return null;
        return new DesiredCompanyResponse(
                desiredCompany.getId(),
                desiredCompany.getDesiredCompany1(),
                desiredCompany.getDesiredCompany2(),
                desiredCompany.getDesiredRegion(),
                desiredCompany.getSalaryType(),
                desiredCompany.getDesiredSalary(),
                desiredCompany.getCareerPlan()
        );
    }
}

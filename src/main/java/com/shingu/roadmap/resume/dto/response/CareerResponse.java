package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Career;

public record CareerResponse(
        String companyName,
        PeriodResponse period,
        String department,
        String description
) {
    public static CareerResponse from(Career c) {
        if (c == null) return null;
        return new CareerResponse(
                c.getCompanyName() != null ? c.getCompanyName().displayName() : null,
                PeriodResponse.from(c.getPeriod()),
                c.getDepartment() != null ? c.getDepartment() : null,
                c.getDescription()
        );
    }
}

package com.shingu.roadmap.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학력 수준을 나타내는 Enum")
public enum EducationLevelType {
    ELEMENTARY("초등학교"),
    MIDDLE("중학교"),
    HIGH("고등학교 졸업"),
    COLLEGE("(2/3년제 대학교"),
    UNIVERSITY("4년제 대학교"),
    GRADUATE_SCHOOL("대학원");

    private final String description;

    EducationLevelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

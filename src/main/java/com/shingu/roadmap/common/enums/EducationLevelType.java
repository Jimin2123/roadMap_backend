package com.shingu.roadmap.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "학력 수준을 나타내는 Enum")
public enum EducationLevelType {
    NO_REQUIREMENT(0, "학력무관"),
    HIGH_SCHOOL(1, "고등학교졸업"),
    ASSOCIATE_DEGREE(2, "대학졸업(2,3년)"),
    BACHELOR(3, "대학교졸업(4년)"),
    MASTER(4, "석사졸업"),
    DOCTORATE(5, "박사졸업"),
    HIGH_SCHOOL_OR_ABOVE(6, "고등학교졸업이상"),
    ASSOCIATE_OR_ABOVE(7, "대학졸업(2,3년)이상"),
    BACHELOR_OR_ABOVE(8, "대학교졸업(4년)이상"),
    MASTER_OR_ABOVE(9, "석사졸업이상");

    private final int code;
    private final String description;

    EducationLevelType(int code, String description) {
        this.code = code;
        this.description = description;
    }

  public static EducationLevelType fromCode(int code) {
        for (EducationLevelType level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown education code: " + code);
    }
}

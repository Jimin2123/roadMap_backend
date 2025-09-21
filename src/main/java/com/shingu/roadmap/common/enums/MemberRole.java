package com.shingu.roadmap.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    USER("USER", "일반 사용자"),
    ADMIN("ADMIN", "관리자");

    private final String value;
    private final String description;

    public static MemberRole fromValue(String value) {
        for (MemberRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role value: " + value);
    }
}
package com.shingu.roadmap.member.exception;

public enum MemberErrorCode {
    // 회원 관련 에러 코드
    MEMBER_001("MEMBER_001", "존재하지 않는 회원입니다."),
    MEMBER_002("MEMBER_002", "프로필이 존재하지 않습니다."),
    MEMBER_003("MEMBER_003", "이미 존재하는 회원입니다."),
    MEMBER_004("MEMBER_004", "회원 정보 업데이트에 실패했습니다."),
    MEMBER_005("MEMBER_005", "자격증을 찾을 수 없습니다."),
    MEMBER_006("MEMBER_006", "직무 코드가 존재하지 않습니다."),
    MEMBER_007("MEMBER_007", "스킬을 찾을 수 없습니다."),
    MEMBER_008("MEMBER_008", "프로필 생성에 실패했습니다.");

    private final String code;
    private final String message;

    MemberErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
package com.shingu.roadmap.member.exception;

public class ProfileCreationException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_008";

    public ProfileCreationException() {
        super("프로필 생성에 실패했습니다.");
    }

    public ProfileCreationException(String message) {
        super(message);
    }

    public ProfileCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
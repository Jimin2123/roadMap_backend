package com.shingu.roadmap.member.exception;

public class MemberUpdateException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_004";

    public MemberUpdateException() {
        super("회원 정보 업데이트에 실패했습니다.");
    }

    public MemberUpdateException(String message) {
        super(message);
    }

    public MemberUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
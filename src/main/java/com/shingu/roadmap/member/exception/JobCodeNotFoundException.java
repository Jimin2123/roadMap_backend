package com.shingu.roadmap.member.exception;

public class JobCodeNotFoundException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_006";

    public JobCodeNotFoundException() {
        super("직무 코드가 존재하지 않습니다.");
    }

    public JobCodeNotFoundException(String jobCode) {
        super("직무 코드가 존재하지 않습니다: " + jobCode);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
package com.shingu.roadmap.member.exception;

public class DuplicateMemberException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_003";

    public DuplicateMemberException() {
        super("이미 존재하는 회원입니다.");
    }

    public DuplicateMemberException(String email) {
        super("이미 존재하는 회원입니다. email=" + email);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
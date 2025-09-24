package com.shingu.roadmap.member.exception;

public class MemberNotFoundException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_001";

    public MemberNotFoundException() {
        super("존재하지 않는 회원입니다.");
    }

    public MemberNotFoundException(Long memberId) {
        super("존재하지 않는 회원입니다. id=" + memberId);
    }

    public MemberNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
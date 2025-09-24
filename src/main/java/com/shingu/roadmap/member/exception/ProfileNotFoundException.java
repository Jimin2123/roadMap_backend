package com.shingu.roadmap.member.exception;

public class ProfileNotFoundException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_002";

    public ProfileNotFoundException() {
        super("프로필이 존재하지 않습니다.");
    }

    public ProfileNotFoundException(Long memberId) {
        super("프로필이 존재하지 않습니다. memberId=" + memberId);
    }

    public ProfileNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
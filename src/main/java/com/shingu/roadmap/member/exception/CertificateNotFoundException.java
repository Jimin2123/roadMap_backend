package com.shingu.roadmap.member.exception;

public class CertificateNotFoundException extends MemberException {

    private static final String ERROR_CODE = "MEMBER_005";

    public CertificateNotFoundException() {
        super("자격증을 찾을 수 없습니다.");
    }

    public CertificateNotFoundException(String certificateName) {
        super("자격증을 찾을 수 없습니다: " + certificateName);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
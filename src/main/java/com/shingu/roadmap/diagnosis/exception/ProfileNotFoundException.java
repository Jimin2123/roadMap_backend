package com.shingu.roadmap.diagnosis.exception;

/**
 * 회원의 프로필을 찾을 수 없는 경우 발생하는 예외
 */
public class ProfileNotFoundException extends DiagnosisException {

    public ProfileNotFoundException(Long memberId) {
        super(
                "PROFILE_NOT_FOUND",
                String.format("회원의 프로필을 찾을 수 없습니다. memberId: %d. 프로필을 먼저 작성해주세요.", memberId)
        );
    }
}

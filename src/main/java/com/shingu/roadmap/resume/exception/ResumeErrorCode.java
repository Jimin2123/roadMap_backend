package com.shingu.roadmap.resume.exception;

public enum ResumeErrorCode {
    // 이력서 관련 에러 코드
    RESUME_001("RESUME_001", "이력서가 존재하지 않습니다."),
    RESUME_002("RESUME_002", "이력서 요청 데이터가 없습니다."),
    RESUME_003("RESUME_003", "이력서 생성에 실패했습니다."),
    RESUME_004("RESUME_004", "이력서 업데이트에 실패했습니다."),
    RESUME_005("RESUME_005", "프로젝트를 찾을 수 없습니다."),
    RESUME_006("RESUME_006", "활동 내역을 찾을 수 없습니다."),
    RESUME_007("RESUME_007", "학력 정보를 찾을 수 없습니다."),
    RESUME_008("RESUME_008", "자기소개를 찾을 수 없습니다."),
    RESUME_009("RESUME_009", "잘못된 기간 정보입니다."),
    RESUME_010("RESUME_010", "스킬 정보가 올바르지 않습니다.");

    private final String code;
    private final String message;

    ResumeErrorCode(String code, String message) {
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
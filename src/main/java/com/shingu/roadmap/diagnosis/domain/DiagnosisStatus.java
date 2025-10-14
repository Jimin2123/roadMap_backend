package com.shingu.roadmap.diagnosis.domain;

public enum DiagnosisStatus {
    PENDING,        // 대기 중
    IN_PROGRESS,    // 진행 중
    COMPLETED,      // 완료
    FAILED,         // 실패
    AWAITING_USER_INPUT  // 사용자 입력 대기 (Human-in-the-loop)
}
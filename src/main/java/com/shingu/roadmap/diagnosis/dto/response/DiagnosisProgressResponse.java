package com.shingu.roadmap.diagnosis.dto.response;

import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역량 진단 진행 상태 응답 DTO")
public record DiagnosisProgressResponse(
        @Schema(description = "진단 ID", example = "1")
        Long diagnosisId,

        @Schema(description = "진단 상태", example = "IN_PROGRESS")
        DiagnosisStatus status,

        @Schema(description = "현재 진행 단계", example = "RESUME_ANALYSIS")
        DiagnosisStep currentStep,

        @Schema(description = "진행률 (0-100)", example = "20")
        Integer progressPercentage,

        @Schema(description = "현재 진행 상태 메시지", example = "이력서를 분석하고 있습니다...")
        String currentMessage
) {
}
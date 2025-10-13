package com.shingu.roadmap.diagnosis.dto.response;

import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역량 진단 시작 응답 DTO")
public record DiagnosisStartResponse(
        @Schema(description = "생성된 진단 ID", example = "1")
        Long diagnosisId,

        @Schema(description = "진단 상태", example = "PENDING")
        DiagnosisStatus status,

        @Schema(description = "응답 메시지", example = "진단이 시작되었습니다.")
        String message
) {
}
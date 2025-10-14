package com.shingu.roadmap.diagnosis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "근거 출처 타입")
public enum EvidenceSourceType {
    @Schema(description = "이력서")
    RESUME,

    @Schema(description = "자기소개서")
    COVER_LETTER,

    @Schema(description = "자격증")
    CERTIFICATE,

    @Schema(description = "학력")
    EDUCATION,

    @Schema(description = "경력")
    CAREER,

    @Schema(description = "프로젝트")
    PROJECT,

    @Schema(description = "기술 스택")
    SKILL,

    @Schema(description = "진단 설문")
    DIAGNOSIS_SURVEY,

    @Schema(description = "기타")
    OTHER
}

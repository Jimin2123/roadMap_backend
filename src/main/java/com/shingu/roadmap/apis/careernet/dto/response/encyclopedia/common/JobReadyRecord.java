package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 12. 준비 방법
@Schema(description = "준비 방법")
public record JobReadyRecord(
        @JsonProperty("recruit")
        @Schema(description = "입직 및 취업방법")
        List<RecruitDetailRecord> recruit,

        @JsonProperty("certificate")
        @Schema(description = "관련자격증")
        List<CertificateDetailRecord> certificate,

        @JsonProperty("training")
        @Schema(description = "직업훈련")
        List<TrainingDetailRecord> training,

        @JsonProperty("curriculum")
        @Schema(description = "정규교육과정")
        List<CurriculumDetailRecord> curriculum
) {}
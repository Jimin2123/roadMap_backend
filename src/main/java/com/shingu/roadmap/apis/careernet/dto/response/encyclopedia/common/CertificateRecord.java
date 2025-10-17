package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 5. 관련 자격증
@Schema(description = "관련 자격증")
public record CertificateRecord(
        @JsonProperty("certi") @Schema(description = "관련 자격증명") String certi,
        @JsonProperty("link") @Schema(description = "관련 자격증 링크") String link
) {}
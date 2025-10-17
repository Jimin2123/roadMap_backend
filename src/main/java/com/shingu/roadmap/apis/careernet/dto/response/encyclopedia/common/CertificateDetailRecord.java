package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관련자격증 상세")
public record CertificateDetailRecord(
        @JsonProperty("certificate") String certificate
) {}
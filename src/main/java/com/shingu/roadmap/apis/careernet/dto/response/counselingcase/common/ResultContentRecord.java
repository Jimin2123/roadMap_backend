package com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결과 내용")
public record ResultContentRecord(
        @JsonProperty("code")
        @Schema(description = "결과코드 (0: 성공, 음수: 실패)")
        String code,

        @JsonProperty("message")
        @Schema(description = "처리결과 메시지")
        String message
) {}
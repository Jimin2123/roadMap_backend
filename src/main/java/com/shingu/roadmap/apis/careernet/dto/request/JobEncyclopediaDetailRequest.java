package com.shingu.roadmap.apis.careernet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업백과 상세 조회 요청 DTO")
public record JobEncyclopediaDetailRequest(

        @Schema(description = "OpenAPI 인증키", requiredMode = Schema.RequiredMode.REQUIRED, example = "YOUR_API_KEY")
        String apiKey,

        @Schema(description = "직업코드", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        Integer seq
) {}
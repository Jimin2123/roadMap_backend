package com.shingu.roadmap.apis.careernet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업백과 목록 조회 요청 DTO")
public record JobEncyclopediaListRequest(

        @Schema(description = "OpenAPI 인증키", requiredMode = Schema.RequiredMode.REQUIRED, example = "YOUR_API_KEY")
        String apiKey,

        @Schema(description = "페이지 번호", example = "1")
        Integer pageIndex,

        @Schema(description = "검색어 (직업명)", example = "개발자")
        String searchJobNm,

        @Schema(description = "직업 테마 코드", example = "102428")
        String searchThemeCode,

        @Schema(description = "직업 적성 유형 코드", example = "104740")
        String searchAptdCodes,

        @Schema(description = "직업 분류 코드", example = "1")
        String searchJobCd
) {}
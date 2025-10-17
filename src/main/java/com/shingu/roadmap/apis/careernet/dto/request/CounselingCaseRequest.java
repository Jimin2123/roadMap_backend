package com.shingu.roadmap.apis.careernet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "진로 상담 사례 목록 조회 요청 DTO")
public record CounselingCaseRequest(

        @Schema(description = "OpenAPI 인증키", requiredMode = Schema.RequiredMode.REQUIRED, example = "YOUR_API_KEY")
        String apiKey,

        @Schema(description = "서비스 타입", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "api")
        String svcType,

        @Schema(description = "서비스 코드 (COUNSEL: 리스트, COUNSEL_VIEW: 상세)"
                , requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "COUNSEL")
        String svcCode,

        @Schema(description = "응답 데이터 형식 (xml 또는 json)", defaultValue = "json")
        String contentType,

        @Schema(description = "상담사례 분류 코드")
        String gubun,

        @Schema(description = "상담사례 코드 (상세 내용만)", example = "1")
        String con_cd
) {}
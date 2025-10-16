package com.shingu.roadmap.apis.careernet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업정보 목록 조회 요청 DTO")
public record JobInformationRequest(
        @Schema(description = "OpenAPI 인증키", requiredMode = Schema.RequiredMode.REQUIRED, example = "YOUR_API_KEY")
        String apiKey,

        @Schema(description = "서비스 타입", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "api")
        String svcType,

        @Schema(description = "서비스 코드(JOB: 직업정보, JOB_VIEW: 직업정보 상세)",
                requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "JOB")
        String svcCode,

        @Schema(description = "직업사전 분류형태 코드(job_dic_list: 커리어넷직업분류별, job_apti_list: 적성유형별)",
                requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "job_dic_list")
        String gubun,

        @Schema(description = "응답 데이터 형식", defaultValue = "json")
        String contentType,

        @Schema(description = "능력별 필터(전체일 경우 빈칸)", example = "1")
        String pgubn,

        @Schema(description = "직업분류 또는 직군 필터(전체일 경우 빈칸)", example = "1")
        String category,

        @Schema(description = "현재 페이지")
        String thisPage,

        @Schema(description = "페이지당 건수")
        String perPage,

        @Schema(description = "검색어")
        String searchJobNm,

        @Schema(description = "직업코드 (상세일 경우만 활성화)", example = "1")
        String jobdicSeq
) {}
package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 11. 관련 진로상담
@Schema(description = "관련 진로상담")
public record RelatedCounselingRecord(
        @JsonProperty("cnslt") @Schema(description = "진로상담 내용 요약") String cnslt,
        @JsonProperty("SJ") @Schema(description = "제목") String sj,
        @JsonProperty("CN") @Schema(description = "내용") String cn,
        @JsonProperty("REGIST_DT") @Schema(description = "등록일") String registDt,
        @JsonProperty("cnslt_seq") @Schema(description = "관련진로상담 ID") Long cnsltSeq
) {}
package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관련 진솔리스트")
public record RelJinsolRecord(
        @JsonProperty("SUBJECT") @Schema(description = "제목") String subject,
        @JsonProperty("ALT") @Schema(description = "대체 텍스트") String alt,
        @JsonProperty("THUMBNAIL") @Schema(description = "썸네일") String thumbnail,
        @JsonProperty("SEQ") @Schema(description = "일련번호") Integer seq
) {}
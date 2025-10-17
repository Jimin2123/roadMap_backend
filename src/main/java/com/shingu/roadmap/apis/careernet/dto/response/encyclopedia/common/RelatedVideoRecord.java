package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 10. 관련 동영상
@Schema(description = "관련 동영상")
public record RelatedVideoRecord(
        @JsonProperty("video_name") @Schema(description = "동영상 제목") String videoName,
        @JsonProperty("THUMBNAIL_FILE_SER") @Schema(description = "썸네일 ID") String thumbnailFileSer,
        @JsonProperty("job_cd") @Schema(description = "직업코드") String jobCd,
        @JsonProperty("THUMNAIL_PATH") @Schema(description = "썸네일 URL") String thumbnailPath,
        @JsonProperty("OUTPATH3") @Schema(description = "동영상 URL") String outpath3,
        @JsonProperty("video_id") @Schema(description = "동영상 ID") String videoId,
        @JsonProperty("CID") @Schema(description = "콘텐츠 ID") String cid,
        @JsonProperty("TRGET_SE") @Schema(description = "타겟층") String trgetSe
) {}
package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 18. 수행능력, 지식, 환경
@Schema(description = "수행능력 항목")
public record PerformanceItemRecord(
        // 세부 항목 키(environment, perform, knowledge)가 동적으로 변할 수 있으므로,
        // 이를 모두 필드로 포함하고 사용하는 쪽에서 null 체크를 하는 것이 일반적입니다.
        @JsonProperty("environment") @Schema(description = "업무환경 능력명", nullable = true) String environment,
        @JsonProperty("perform") @Schema(description = "업무수행능력 능력명", nullable = true) String perform,
        @JsonProperty("knowledge") @Schema(description = "지식중요도 능력명", nullable = true) String knowledge,

        @JsonProperty("inform") @Schema(description = "설명") String inform,
        @JsonProperty("importance") @Schema(description = "중요도") Integer importance
) {}
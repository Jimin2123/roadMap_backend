package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 13. 관련 기관
@Schema(description = "관련 기관")
public record RelatedOrganizationRecord(
        @JsonProperty("rel_org") @Schema(description = "관련기관명") String relOrg,
        @JsonProperty("rel_org_url") @Schema(description = "관련기관 URL") String relOrgUrl
) {}

package com.shingu.roadmap.common.controller;

import com.shingu.roadmap.common.dto.response.CertificateAutoCompleteResponse;
import com.shingu.roadmap.common.dto.response.SkillAutoCompleteResponse;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Common API", description = "공통 API")
public interface CommonControllerSwagger {

  @Operation(
          summary = "자격증 자동완성",
          description = "자격증 정보 자동완성 기능을 제공합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "자격증 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<List<CertificateAutoCompleteResponse>> searchCerts(String query);

  @Operation(
          summary = "자격증 자동완성",
          description = "자격증 정보 자동완성 기능을 제공합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "자격증 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<List<SkillAutoCompleteResponse>> searchSkills(String query);
}

package com.shingu.roadmap.task.controller;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Task API", description = "직무, 채용 정보 관련 API")
public interface TaskControllerSwagger {
  @Operation(
          summary = "채용 정보 조회",
          description = "사람인 API를 통해 채용 정보를 조회합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "채용 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = SaraminJobListResponse.class))
                  )
          }
  )
  ResponseEntity<SaraminJobListResponse> getJobList();

  @Operation(
          summary = "인턴 채용 정보 조회",
          description = "사람인 API를 통해 인턴 채용 정보를 조회합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "인턴 채용 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = SaraminJobListResponse.class))
                  )
          }
  )
  ResponseEntity<SaraminJobListResponse> getInternShipList();
}

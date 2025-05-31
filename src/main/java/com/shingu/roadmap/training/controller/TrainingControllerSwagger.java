package com.shingu.roadmap.training.controller;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Training API", description = "교육, 훈련 관련 API")
public interface TrainingControllerSwagger {

  @Operation(
          summary = "고용24 국민 내일 배움 카드 교육 조회",
          description = "고용노동부 고용24 API를 통해 국민 내일 배움 카드 교육 정보를 조회합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "채용 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = SaraminJobListResponse.class))
                  )
          }
  )
  List<TrainingCourseResponse.TrainCourseItem> getTrainingList();

  @Operation(
          summary = "사용자 기반 교육 추천",
          description = "사용자 정보 기반 교육 추천을 위한 API입니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "교육 추천 조회 성공",
                          content = @Content(schema = @Schema(implementation = TrainingCourseResponse.class))
                  )
          }
  )
  List<TrainingCourseResponse.TrainCourseItem> getCoursesForMember(Long id);
}

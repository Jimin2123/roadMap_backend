package com.shingu.roadmap.training.controller;

import com.shingu.roadmap.apis.qnet.dto.response.QnetExamScheduleResponse;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.apis.work24.dto.response.EmpPgmListResponse;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

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
                          content = @Content(
                                  mediaType = "application/json",
                                  array = @ArraySchema(schema = @Schema(implementation = TrainingCourseResponse.TrainCourseItem.class))
                          )
                  )
          }
  )
  List<TrainingCourseResponse.TrainCourseItem> getTrainingList();

  @Operation(
          summary = "사용자 기반 교육 추천",
          description = "사용자 정보 기반 교육 추천을 위한 API입니다.",
          parameters = {
                  @Parameter(name = "memberId", description = "회원 ID", example = "1")
          },
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "교육 추천 조회 성공",
                          content = @Content(
                                  mediaType = "application/json",
                                  array = @ArraySchema(schema = @Schema(implementation = TrainingCourseResponse.TrainCourseItem.class))
                          )
                  )
          }
  )
  ResponseEntity<List<TrainingCourseResponse.TrainCourseItem>> getCoursesForMember(Long memberId);

  @Operation(
          summary = "구직자취업역량 강화프로그램 API",
          description = "고용24 구직자취업역량 강화프로그램 API를 통해 구직자 취업 역량 강화 프로그램 정보를 조회합니다.",
          parameters = {
                  @Parameter(name = "memberId", description = "회원 ID", example = "1")
          },
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "프로그램 조회 성공",
                          content = @Content(
                                  mediaType = "application/json",
                                  array = @ArraySchema(schema = @Schema(implementation = EmpPgmListResponse.EmpPgmSchdInvite.class))
                          )
                  )
          }
  )
  ResponseEntity<List<EmpPgmListResponse.EmpPgmSchdInvite>> getTrainingProgramsForMember(Long memberId);

  @Operation(
          summary = "국가자격 시험일정 목록 조회",
          description = "공공포털 데이터 API를 통해 국가자격 시험일정 목록을 조회합니다.",
          parameters = {
                  @Parameter(name = "qualgbcd",
                          description = "자격 구분 코드 - T : 국가기술자격 - C : 과정평가형자격 - W : 일학습병행자격 - S : 국가전문자격",
                          example = "T"
                  ),
                  @Parameter(name = "jmcd", description = "국가 자격증 코드", example = "1320")
          },
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "시험 일정 조회 성공",
                          content = @Content(
                                  mediaType = "application/json",
                                  array = @ArraySchema(schema = @Schema(implementation = QnetExamScheduleResponse.Item.class))
                          )
                  )
          }
  )
  ResponseEntity<List<QnetExamScheduleResponse.Item>> getQnetExamSchedule(String qualgbcd, String jmcd);
}

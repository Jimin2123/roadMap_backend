package com.shingu.roadmap.training.controller;

import com.shingu.roadmap.apis.qnet.dto.response.QnetExamScheduleResponse;
import com.shingu.roadmap.apis.work24.dto.response.EmpPgmListResponse;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.security.model.CustomUserDetails;
import com.shingu.roadmap.training.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TrainingController implements TrainingControllerSwagger {
  private final TrainingService trainingService;

  @Override
  @GetMapping("/api/v1/training")
  public ResponseEntity<List<TrainingCourseResponse.TrainCourseItem>> getTrainingList() {
    return null;
  }

  @Override
  @GetMapping("/api/v1/training/courses")
  public ResponseEntity<List<TrainingCourseResponse.TrainCourseItem>> getCoursesForMember(
          @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Long memberId = userDetails.getMemberId();

    List<TrainingCourseResponse.TrainCourseItem> response = trainingService.recommendCoursesForMember(memberId);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/api/v1/training/programs")
  public ResponseEntity<List<EmpPgmListResponse.EmpPgmSchdInvite>> getTrainingProgramsForMember(
          @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Long memberId = userDetails.getMemberId();

    List<EmpPgmListResponse.EmpPgmSchdInvite> response = trainingService.getTrainingProgramsForMember(memberId);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/api/v1/certificate/exam-schedule")
  public ResponseEntity<List<QnetExamScheduleResponse.Item>> getQnetExamSchedule(
          @RequestParam() String qualgbcd,
          @RequestParam() String jmcd
  ) {
    List<QnetExamScheduleResponse.Item> response = trainingService.getQnetExamSchedule(qualgbcd, jmcd);
    return ResponseEntity.ok(response);
  }
}

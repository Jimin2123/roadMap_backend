package com.shingu.roadmap.training.controller;

import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.training.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TrainingController implements TrainingControllerSwagger {
  private final TrainingService trainingService;

  @Override
  @GetMapping("/api/v1/training")
  public List<TrainingCourseResponse.TrainCourseItem> getTrainingList() {
    return null;
  }

  @Override
  @GetMapping("/api/v1/training/{id}/courses")
  public List<TrainingCourseResponse.TrainCourseItem> getCoursesForMember(@PathVariable Long id) {
    return trainingService.recommendCoursesForMember(id);
  }
}

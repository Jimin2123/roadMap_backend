package com.shingu.roadmap.task.controller;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TaskController implements TaskControllerSwagger {
  private final TaskService taskService;

  @Override
  @GetMapping("/api/v1/jobs")
  public ResponseEntity<SaraminJobListResponse> getJobList() {
    return null;
  }

  @Override
  @GetMapping("/api/v1/internship")
  public ResponseEntity<SaraminJobListResponse> getInternShipList() {
    return null;
  }
}

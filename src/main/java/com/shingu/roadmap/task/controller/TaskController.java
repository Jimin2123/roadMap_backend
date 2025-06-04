package com.shingu.roadmap.task.controller;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TaskController implements TaskControllerSwagger {
  private final TaskService taskService;

  @Override
  @GetMapping("/api/v1/jobs")
  public ResponseEntity<SaraminJobListResponse> getJobList(
          @RequestParam(name = "page", defaultValue = "0") int page
  ) {
    SaraminJobListResponse response = taskService.getJobListForMainPage(page);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/api/v1/jobs/{memberId}")
  public ResponseEntity<SaraminJobListResponse> getJobListForMember(
          @RequestParam(name = "page", defaultValue = "0") int page,
          @PathVariable Long memberId
  ) {
    SaraminJobListResponse response = taskService.getJobListForMember(page, memberId);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/api/v1/internship")
  public ResponseEntity<SaraminJobListResponse> getInternShipList() {
    return null;
  }
}

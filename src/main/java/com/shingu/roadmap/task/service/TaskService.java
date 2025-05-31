package com.shingu.roadmap.task.service;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.apis.saramin.service.SaraminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
  private final SaraminService saraminService;

  public SaraminJobListResponse getJobListForMainPage() {
    return saraminService.getJobListForMainPage();
  }
}

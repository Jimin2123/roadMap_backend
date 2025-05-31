package com.shingu.roadmap.apis.saramin.service;

import com.shingu.roadmap.apis.saramin.client.SaraminClient;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaraminService {

  private final SaraminClient saraminClient;

  public SaraminJobListResponse getJobList(String keyword) {
    return saraminClient.getJobList(keyword);
  }

  public SaraminJobListResponse getJobListForMainPage() {
    return saraminClient.getJobList("");
  }
}

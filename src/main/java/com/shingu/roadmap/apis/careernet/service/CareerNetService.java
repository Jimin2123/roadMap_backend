package com.shingu.roadmap.apis.careernet.service;

import com.shingu.roadmap.apis.careernet.client.CareerNetClient;
import com.shingu.roadmap.apis.careernet.dto.request.JobEncyclopediaRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobInformationRequest;
import com.shingu.roadmap.apis.careernet.dto.response.JobDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.JobEncyclopediaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CareerNetService {
  private final CareerNetClient careerNetClient;

  /**
   * 직업백과 목록 조회
   * @param JobEncyclopediaRequest
   * @return
   */
  public JobEncyclopediaResponse getJobEncyclopediaList(JobEncyclopediaRequest request) {
    return careerNetClient.getJobEncyclopediaList(request);
  }

  /**
   * 직업 상세 정보 조회
   * @param JobInformationRequest
   * @return
   */
  public JobDetailResponse getJobInformation(JobInformationRequest request) {
    return careerNetClient.getJobInformation(request);
  }
}

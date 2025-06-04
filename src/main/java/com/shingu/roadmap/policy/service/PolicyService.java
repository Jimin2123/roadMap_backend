package com.shingu.roadmap.policy.service;

import com.shingu.roadmap.apis.youthPolicy.client.YouthPolicyClient;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {
  private final YouthPolicyClient youthPolicyClient;

  public YouthPolicyListResponse getYouthPolicyList() {
    return youthPolicyClient.getYouthPolicyList(12797, 1);
  }
}

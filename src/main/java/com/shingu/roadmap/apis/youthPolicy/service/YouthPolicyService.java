package com.shingu.roadmap.apis.youthPolicy.service;

import com.shingu.roadmap.apis.youthPolicy.client.YouthPolicyClient;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyItemResponse;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YouthPolicyService {
  private final YouthPolicyClient youthPolicyClient;

  public YouthPolicyListResponse getAllYouthPolicies(int page, int size, int zoneCode) {
    return this.youthPolicyClient.getYouthPolicyList(page, size, zoneCode);
  }
}

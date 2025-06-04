package com.shingu.roadmap.policy.controller;

import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import com.shingu.roadmap.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PolicyController implements PolicyControllerSwagger {
  private final PolicyService policyService;

  @Override
  @GetMapping("/api/v1/policy")
  public YouthPolicyListResponse getYouthPolicyList() {
    return policyService.getYouthPolicyList();
  }
}

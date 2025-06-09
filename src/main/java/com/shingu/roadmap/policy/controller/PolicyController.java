package com.shingu.roadmap.policy.controller;

import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyItemResponse;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import com.shingu.roadmap.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PolicyController implements PolicyControllerSwagger {
  private final PolicyService policyService;

  @Override
  @GetMapping("/api/v1/policy")
  public ResponseEntity<List<YouthPolicyItemResponse>> getYouthPolicyList(
          @RequestParam(value = "page", defaultValue = "1") int page,
          @RequestParam(value = "size", defaultValue = "20") int size
  ) {
    List<YouthPolicyItemResponse> response =  policyService.getYouthPolicyList(page, size);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/api/v1/policy/{memberId}")
  public YouthPolicyListResponse getPolicyListByUserInfo(@PathVariable Long memberId) {
    return null;
  }
}

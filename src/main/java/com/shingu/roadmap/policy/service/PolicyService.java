package com.shingu.roadmap.policy.service;

import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyItemResponse;
import com.shingu.roadmap.apis.youthPolicy.service.YouthPolicyService;
import com.shingu.roadmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {
  private final YouthPolicyService youthPolicyService;
  private final MemberRepository memberRepository;

  public List<YouthPolicyItemResponse> getYouthPolicyList(int page, int size) {
    return youthPolicyService.getAllYouthPolicies(page, size);
  }
}

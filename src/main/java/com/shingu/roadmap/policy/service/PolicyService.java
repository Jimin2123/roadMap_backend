package com.shingu.roadmap.policy.service;

import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import com.shingu.roadmap.apis.youthPolicy.service.YouthPolicyService;
import com.shingu.roadmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {
  private final YouthPolicyService youthPolicyService;
  private final MemberRepository memberRepository;

  public YouthPolicyListResponse getYouthPolicyList(int page, int size) {
    return youthPolicyService.getAllYouthPolicies(page, size, 0);
  }

  public YouthPolicyListResponse getYouthPolicyListByUserInfo(Long memberId, int page, int size) {
    return memberRepository.findById(memberId)
            .map(member -> youthPolicyService.getAllYouthPolicies(page, size, Integer.parseInt(member.getAddress().getZonecode())))
            .orElseThrow(() -> new IllegalArgumentException("멤버 ID를 찾을 수 없습니다 : " + memberId));
  }
}

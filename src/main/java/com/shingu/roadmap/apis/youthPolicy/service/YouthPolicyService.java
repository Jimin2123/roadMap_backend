package com.shingu.roadmap.apis.youthPolicy.service;

import com.shingu.roadmap.apis.youthPolicy.client.YouthPolicyClient;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyItemResponse;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouthPolicyService {
  private final YouthPolicyClient youthPolicyClient;

  /**
   * 청년 정책 목록 조회 (캐싱 적용)
   *
   * 캐시 키: page + size + zoneCode
   * TTL: 2시간 (application.yml 설정) - 정책은 자주 변경되지 않음
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param zoneCode 지역 코드 (0: 전체)
   * @return 청년 정책 목록 응답
   */
  @Cacheable(value = "youthPolicyList",
             key = "#page + '_' + #size + '_' + #zoneCode",
             unless = "#result == null || #result.result() == null || #result.result().youthPolicyList() == null || #result.result().youthPolicyList().isEmpty()")
  public YouthPolicyListResponse getAllYouthPolicies(int page, int size, int zoneCode) {
    log.debug("Fetching youth policy list from external API (page: {}, size: {}, zoneCode: {})", page, size, zoneCode);
    return this.youthPolicyClient.getYouthPolicyList(page, size, zoneCode);
  }
}

package com.shingu.roadmap.apis.youthPolicy.client;

import com.shingu.roadmap.apis.youthPolicy.config.YouthPolicyProperties;
import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class YouthPolicyClient {
  private final YouthPolicyProperties youthPolicyProperties;
  private final RestClient restClient;

  YouthPolicyClient(@Qualifier("youthPolicyRestClient") RestClient restClient,
                    YouthPolicyProperties properties) {
    this.youthPolicyProperties = properties;
    this.restClient = restClient;
  }

  public YouthPolicyListResponse getYouthPolicyList(int zoneCode, int pageNum) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(youthPolicyProperties.getBaseUrl())
            .queryParam("apiKeyNm", youthPolicyProperties.getApiKey())
            .queryParam("pageNum", pageNum)
            .queryParam("pageSize", 100)
            .queryParam("pageType", "1")
            .queryParam("zipCd", String.valueOf(zoneCode))
            .queryParam("rtnType", "JSON");

    String uri = builder.build(false).encode().toUriString();
    System.out.print(uri);

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(YouthPolicyListResponse.class);
  }
}

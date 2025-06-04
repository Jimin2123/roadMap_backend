package com.shingu.roadmap.apis.saramin.client;

import com.shingu.roadmap.apis.saramin.config.SaraminApiProperties;
import com.shingu.roadmap.apis.saramin.domain.SaraminRegion;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.common.enums.EducationLevelType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@Component
public class SaraminClient {
  private SaraminApiProperties properties;
  private final RestClient restClient;

  SaraminClient(@Qualifier("saraminRestClient") RestClient restClient, SaraminApiProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public SaraminJobListResponse getJobList(
          Set<String> keyword, int page, SaraminRegion region, Set<Integer> groupCodes,
          Set<Integer> jobCodes, EducationLevelType educationLevelType
          ) {

    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(properties.getBaseUrl())
            .queryParam("access-key", properties.getApiKey())
            .queryParam("count", 20)
            .queryParam("sort", "pd");

//    if(keyword != null && !keyword.isEmpty()) {
//      builder.queryParam("keywords", keyword);
//    }

    if(page > 0) {
      builder.queryParam("start", page); // 페이지 시작 위치
    }

    if(groupCodes != null && !groupCodes.isEmpty()) {
      builder.queryParam("job_mid_cd", String.join(",", groupCodes.stream().map(String::valueOf).toList()));
    }

    if(jobCodes != null && !jobCodes.isEmpty()) {
      builder.queryParam("job_cd", String.join(",", jobCodes.stream().map(String::valueOf).toList()));
    }

    if(region != null) {
      if(region.getRegionCode1() == 101000) {
        builder.queryParam("loc_cd", region.getRegionCode1());
      }else {
        builder.queryParam("loc_cd", region.getRegionCode1() + ", 101000");
      }
    }

    if(educationLevelType != null) {
      builder.queryParam("edu_lv", educationLevelType.getCode()); // 학력 조건
    }

    URI uri = builder.build(false).encode().toUri();

    System.out.println(uri);

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(SaraminJobListResponse.class);
  }
}

package com.shingu.roadmap.apis.saramin.client;

import com.shingu.roadmap.apis.saramin.config.SaraminApiProperties;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class SaraminClient {
  private SaraminApiProperties properties;
  private final RestClient restClient;

  SaraminClient(@Qualifier("saraminRestClient") RestClient restClient, SaraminApiProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public SaraminJobListResponse getJobList(String keyword) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(properties.getBaseUrl())
            .queryParam("access-key", properties.getApiKey())
            .queryParam("keywords", keyword)
            .queryParam("loc_cd", "101010")
            .queryParam("job_mid_cd", "2")
            .queryParam("job_cd", "2248");
//            .queryParam("job_type", "2")
//            .queryParam("edu_lv", "3")
//            .queryParam("count", 10)
//            .queryParam("sort", "pd");

    URI uri = builder.build(false).encode().toUri();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(SaraminJobListResponse.class);
  }


}

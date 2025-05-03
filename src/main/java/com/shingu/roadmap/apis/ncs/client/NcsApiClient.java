package com.shingu.roadmap.apis.ncs.client;

import com.shingu.roadmap.apis.ncs.config.NcsApiProperties;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class NcsApiClient {

  private final NcsApiProperties ncsApiProperties;
  private final RestClient restClient;

  public NcsOccupationResponse getOccupation(String ncsCode) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi2.do")
            .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 10)
            .queryParam("returnType", "JSON")
            .queryParam("dutyCd", ncsCode);

    URI url = builder.build(true).encode().toUri();

    return restClient.get()
            .uri(url)
            .retrieve()
            .body(NcsOccupationResponse.class);
  }
}

package com.shingu.roadmap.apis.ncs.client;

import com.shingu.roadmap.apis.ncs.config.NcsApiProperties;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsTrainingStandardResponse;
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

  /**
   * NCS 직무정보 API 호출
   *
   * @param ncsCode NCS 코드
   * @return NCS 직무정보 응답 DTO
   */
  public NcsOccupationResponse getOccupation(String ncsCode) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi2.do")
            .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 10)
            .queryParam("returnType", "JSON")
            .queryParam("dutyCd", ncsCode);

    URI uri = builder.build(true).encode().toUri();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(NcsOccupationResponse.class);
  }

  /**
   * NCS 훈련기준 고려사항 API 호출
   *
   * @param ncsCode NCS 코드
   * @return NcsTrainingStandardResponse - NCS 훈련기준 응답 DTO
   */
  public NcsTrainingStandardResponse getNcsTrainingStandard(String ncsCode) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi11.do")
            .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
            .queryParam("returnType", "JSON")
            .queryParam("dutyCd", ncsCode);

    URI uri = builder.build(true).encode().toUri();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(NcsTrainingStandardResponse.class);
  }
}

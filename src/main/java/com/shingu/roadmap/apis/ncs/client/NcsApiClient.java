package com.shingu.roadmap.apis.ncs.client;

import com.shingu.roadmap.apis.ncs.config.NcsApiProperties;
import com.shingu.roadmap.apis.ncs.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class NcsApiClient {

  private final NcsApiProperties ncsApiProperties;
  private final RestClient restClient;

  NcsApiClient(@Qualifier("ncsRestClient") RestClient restClient, NcsApiProperties ncsApiProperties) {
    this.restClient = restClient;
    this.ncsApiProperties = ncsApiProperties;
  }

  /**
   * NCS 직무정보 API
   * @apiNote NCS직무코드로 요청하면 직무명, 직무정의 등 NCS 직무에 대한 정보를 조회 할 수 있다.
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
   * NCS 훈련기준 고려사항 API
   * @apiNote NCS직무코드로 요청하면 훈련기준 고려사항(관련자격종목, 작업활동영역,
   * NCS 관련 직종, 관련 홈페이지 안내) 정보를 조회 할 수 있다.
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

  /**
   * NCS 직책 조회 API
   * @apiNote  NCS직무코드 요청하면 직책(직책 수준, 능력단위) 정보를 조회 할 수 있다.
   * @param ncsCode NCS 코드
   * @return NcsJobPositionResponse - NCS 직책 응답 DTO
   */
  public NcsJobPositionResponse getNcsJobPosition(String ncsCode) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi9.do")
            .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
            .queryParam("returnType", "JSON")
            .queryParam("dutyCd", ncsCode);
    URI uri = builder.build(true).encode().toUri();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(NcsJobPositionResponse.class);
  }

  /**
   * NCS 능력단위	API
   * @apiNote NCS직무코드로 요청하면 능력단위 명칭, 분류번호, 능력단위의 정의 등 능력단위에 대한 정보를 조회 할 수 있다.
   * @param ncsCode NCS 코드
   */
  public NcsCompUnitResponse getNcsCompetencyUnit(String ncsCode) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi3.do")
            .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
            .queryParam("returnType", "JSON")
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 10)
            .queryParam("dutyCd", ncsCode);

    URI uri = builder.build(true).encode().toUri();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(NcsCompUnitResponse.class);
  }

  /**
   * NCS 수행준거 KSA(지식/기술/태도) API
   * @apiNote NCS직무코드와 능력단위코드로 요청하면 수행준거와 KSA(지식/기술/태도) 정보를 조회 할 수 있다.
   * @param ncsCode
   * @param compUnitCd
   */
  public NcsKsaResponse getNcsKsaByDutyCode(String ncsCode, String compUnitCd) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi5.do")
            .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
            .queryParam("returnType", "JSON")
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 10)
            .queryParam("dutyCd", ncsCode)
            .queryParam("compUnitCd", compUnitCd);

    URI uri = builder.build(true).encode().toUri();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(NcsKsaResponse.class);
  }
}

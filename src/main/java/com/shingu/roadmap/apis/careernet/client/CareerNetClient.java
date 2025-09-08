package com.shingu.roadmap.apis.careernet.client;

import com.shingu.roadmap.apis.careernet.config.CareerNetProperties;
import com.shingu.roadmap.apis.careernet.dto.request.JobEncyclopediaRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobInformationRequest;
import com.shingu.roadmap.apis.careernet.dto.response.JobDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.JobEncyclopediaResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CareerNetClient {
  private final CareerNetProperties careerNetProperties;
  private final RestClient restClient;

  public CareerNetClient(@Qualifier("careerNetRestClient") RestClient restClient, CareerNetProperties properties) {
    this.careerNetProperties = properties;
    this.restClient = restClient;
  }

  public JobEncyclopediaResponse getJobEncyclopediaList(JobEncyclopediaRequest request) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(careerNetProperties.getBaseUrl() + "/front/openapi/jobs.json")
            .queryParam("apiKeyNm", careerNetProperties.getApiKey())
            .queryParam("pageIndex", request.getPageIndex())
            .queryParam("searchJobNm", request.getSearchJobNm())
            .queryParam("searchThemeCode", request.getSearchThemeCode())
            .queryParam("searchAptdCodes", request.getSearchAptdCodes())
            .queryParam("searchJobCd", request.getSearchJobCd());

    String uri = builder.build(false).encode().toUriString();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(JobEncyclopediaResponse.class);
  }

  public JobDetailResponse getJobInformation(JobInformationRequest request) {
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(careerNetProperties.getBaseUrl() + "/openapi/getOpenApi.json")
            .queryParam("apiKey", careerNetProperties.getApiKey())
            .queryParam("svcType", request.getSvcType())
            .queryParam("svcCode", request.getSvcCode())
            .queryParam("contentType", request.getContentType())
            .queryParam("gubun", request.getGubun())
            .queryParam("pgubn", request.getPgubn())
            .queryParam("category", request.getCategory())
            .queryParam("thisPage", request.getThisPage())
            .queryParam("perPage", request.getPerPage())
            .queryParam("searchJobNm", request.getSearchJobNm())
            .queryParam("jobdicSeq", request.getJobdicSeq());

    String uri = builder.build(false).encode().toUriString();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(JobDetailResponse.class);
  }
}
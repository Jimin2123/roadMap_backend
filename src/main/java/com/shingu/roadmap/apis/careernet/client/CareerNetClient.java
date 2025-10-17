package com.shingu.roadmap.apis.careernet.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.careernet.config.CareerNetProperties;
import com.shingu.roadmap.apis.careernet.dto.request.CounselingCaseRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobEncyclopediaDetailRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobEncyclopediaListRequest;
import com.shingu.roadmap.apis.careernet.dto.request.JobInformationRequest;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.CounselingCaseDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.CounselingCaseListResponse;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.JobEncyclopediaDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.JobEncyclopediaListResponse;
import com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@Slf4j
public class CareerNetClient {
    private final RestClient restClient;
    private final CareerNetProperties careerNetProperties;
    private final ObjectMapper objectMapper;

    public CareerNetClient(@Qualifier("careerNetRestClient") RestClient restClient,
                           CareerNetProperties careerNetProperties,
                           ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.careerNetProperties = careerNetProperties;
        this.objectMapper = objectMapper;
    }

    private String getApiKey(String requestApiKey) {
        return requestApiKey != null ? requestApiKey : careerNetProperties.getApiKey();
    }

    public JobEncyclopediaListResponse getJobEncyclopediaList(JobEncyclopediaListRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(careerNetProperties.getBaseUrl() + "/front/openapi/jobs.json")
                .queryParam("apiKey", getApiKey(request.apiKey()))
                .queryParam("pageIndex", request.pageIndex())
                .queryParam("searchJobNm", request.searchJobNm())
                .queryParam("searchThemeCode", request.searchThemeCode())
                .queryParam("searchAptdCodes", request.searchAptdCodes())
                .queryParam("searchJobCd", request.searchJobCd());

        String uri = builder.build(false).encode().toUriString();
        log.debug("CareerNet API Request [JobEncyclopediaList]: {}", uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(JobEncyclopediaListResponse.class);
    }

    public JobEncyclopediaDetailResponse getJobEncyclopediaDetail(JobEncyclopediaDetailRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(careerNetProperties.getBaseUrl() + "/front/openapi/job.json")
                .queryParam("apiKey", getApiKey(request.apiKey()))
                .queryParam("seq", request.seq());

        String uri = builder.build(false).encode().toUriString();
        log.debug("CareerNet API Request [JobEncyclopediaDetail]: {}", uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(JobEncyclopediaDetailResponse.class);
    }

    public JobInfoListResponse getJobInfoList(JobInformationRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(careerNetProperties.getBaseUrl() + "/openapi/getOpenApi.json")
                .queryParam("apiKey", getApiKey(request.apiKey()))
                .queryParam("svcType", request.svcType())
                .queryParam("svcCode", request.svcCode())
                .queryParam("contentType", request.contentType())
                .queryParam("gubun", request.gubun())
                .queryParam("pgubn", request.pgubn())
                .queryParam("category", request.category())
                .queryParam("thisPage", request.thisPage())
                .queryParam("perPage", request.perPage())
                .queryParam("searchJobNm", request.searchJobNm());

        URI uri = builder.build().toUri();
        log.debug("CareerNet API Request [JobInfoList]: {}", uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(JobInfoListResponse.class);
    }

    public JobInfoDetailResponse getJobInfoDetail(JobInformationRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(careerNetProperties.getBaseUrl() + "/openapi/getOpenApi.json")
                .queryParam("apiKey", getApiKey(request.apiKey()))
                .queryParam("svcType", request.svcType())
                .queryParam("svcCode", request.svcCode())
                .queryParam("contentType", request.contentType())
                .queryParam("gubun", request.gubun())
                .queryParam("pgubn", request.pgubn())
                .queryParam("category", request.category())
                .queryParam("thisPage", request.thisPage())
                .queryParam("perPage", request.perPage())
                .queryParam("jobdicSeq", request.jobdicSeq());

        URI uri = builder.build().toUri();
        log.debug("CareerNet API Request [JobInfoDetail]: {}", uri);

        String responseBody = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("Raw CareerNet JobInfoDetail Response: {}", responseBody);

        try {
            return objectMapper.readValue(responseBody, JobInfoDetailResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse JobInfoDetailResponse: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JobInfoDetailResponse", e);
        }
    }

    public CounselingCaseListResponse getCounselingCaseList(CounselingCaseRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(careerNetProperties.getBaseUrl() + "/openapi/getOpenApi")
                .queryParam("apiKey", getApiKey(request.apiKey()))
                .queryParam("svcType", request.svcType())
                .queryParam("svcCode", request.svcCode())
                .queryParam("contentType", request.contentType())
                .queryParam("gubun", request.gubun());

        String uri = builder.build(false).encode().toUriString();
        log.debug("CareerNet API Request [CounselingCaseList]: {}", uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(CounselingCaseListResponse.class);
    }

    public CounselingCaseDetailResponse getCounselingCaseDetail(CounselingCaseRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(careerNetProperties.getBaseUrl() + "/openapi/getOpenApi")
                .queryParam("apiKey", getApiKey(request.apiKey()))
                .queryParam("svcType", request.svcType())
                .queryParam("svcCode", request.svcCode())
                .queryParam("contentType", request.contentType())
                .queryParam("con_cd", request.con_cd());

        String uri = builder.build(false).encode().toUriString();
        log.debug("CareerNet API Request [CounselingCaseDetail]: {}", uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(CounselingCaseDetailResponse.class);
    }
}
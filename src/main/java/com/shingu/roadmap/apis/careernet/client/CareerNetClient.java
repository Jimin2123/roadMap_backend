package com.shingu.roadmap.apis.careernet.client;

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

    public CareerNetClient(@Qualifier("careerNetRestClient") RestClient restClient,
                           CareerNetProperties careerNetProperties) {
        this.restClient = restClient;
        this.careerNetProperties = careerNetProperties;
    }

    /**
     * API 키를 가져오는 헬퍼 메서드
     * request의 apiKey가 null이면 properties의 apiKey를 사용
     */
    private String getApiKey(String requestApiKey) {
        return requestApiKey != null ? requestApiKey : careerNetProperties.getApiKey();
    }

    /**
     * 직업백과 목록 조회
     * @param request 직업백과 목록 조회 요청 DTO
     * @return 직업백과 목록 응답 DTO
     */
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

    /**
     * 직업백과 상세 조회
     * @param request 직업백과 상세 조회 요청 DTO
     * @return 직업백과 상세 응답 DTO
     */
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

    /**
     * 직업 정보 목록 조회
     * @param request 직업 정보 조회 요청 DTO
     * @return 직업 정보 목록 응답 DTO
     */
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

    /**
     * 직업 정보 상세 조회
     * @param request 직업 정보 조회 요청 DTO (jobdicSeq 포함)
     * @return 직업 정보 상세 응답 DTO
     */
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

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(JobInfoDetailResponse.class);
    }

    /**
     * 진로 상담 사례 목록 조회
     * @param request 진로 상담 사례 조회 요청 DTO
     * @return 진로 상담 사례 목록 응답 DTO
     */
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

    /**
     * 진로 상담 사례 상세 조회
     * @param request 진로 상담 사례 조회 요청 DTO (con_cd 포함)
     * @return 진로 상담 사례 상세 응답 DTO
     */
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

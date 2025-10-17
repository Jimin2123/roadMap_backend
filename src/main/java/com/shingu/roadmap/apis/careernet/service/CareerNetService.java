package com.shingu.roadmap.apis.careernet.service;

import com.shingu.roadmap.apis.careernet.client.CareerNetClient;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CareerNetService {
    private final CareerNetClient careerNetClient;

    /**
     * 직업백과 목록 조회
     * @param request 직업백과 목록 조회 요청 DTO
     * @return 직업백과 목록 응답 DTO
     */
    public JobEncyclopediaListResponse getJobEncyclopediaList(JobEncyclopediaListRequest request) {
        return careerNetClient.getJobEncyclopediaList(request);
    }

    /**
     * 직업백과 상세 조회
     * @param request 직업백과 상세 조회 요청 DTO
     * @return 직업백과 상세 응답 DTO
     */
    public JobEncyclopediaDetailResponse getJobEncyclopediaDetail(JobEncyclopediaDetailRequest request) {
        return careerNetClient.getJobEncyclopediaDetail(request);
    }

    /**
     * 직업 정보 목록 조회
     * @param request 직업 정보 조회 요청 DTO
     * @return 직업 정보 목록 응답 DTO
     */
    public JobInfoListResponse getJobInfoList(JobInformationRequest request) {
        return careerNetClient.getJobInfoList(request);
    }

    /**
     * 직업 정보 상세 조회
     * @param request 직업 정보 조회 요청 DTO (jobdicSeq 포함)
     * @return 직업 정보 상세 응답 DTO
     */
    public JobInfoDetailResponse getJobInfoDetail(JobInformationRequest request) {
        return careerNetClient.getJobInfoDetail(request);
    }

    /**
     * 진로 상담 사례 목록 조회
     * @param request 진로 상담 사례 조회 요청 DTO
     * @return 진로 상담 사례 목록 응답 DTO
     */
    public CounselingCaseListResponse getCounselingCaseList(CounselingCaseRequest request) {
        return careerNetClient.getCounselingCaseList(request);
    }

    /**
     * 진로 상담 사례 상세 조회
     * @param request 진로 상담 사례 조회 요청 DTO (con_cd 포함)
     * @return 진로 상담 사례 상세 응답 DTO
     */
    public CounselingCaseDetailResponse getCounselingCaseDetail(CounselingCaseRequest request) {
        return careerNetClient.getCounselingCaseDetail(request);
    }
}

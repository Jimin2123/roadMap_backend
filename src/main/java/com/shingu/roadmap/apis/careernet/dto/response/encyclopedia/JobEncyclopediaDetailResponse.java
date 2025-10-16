package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "직업백과 상세 응답 DTO")
public record JobEncyclopediaDetailResponse(
        @JsonProperty("baseInfo")
        @Schema(description = "직업 기본 정보")
        BaseInfoRecord baseInfo,

        @JsonProperty("workList")
        @Schema(description = "하는 일 목록")
        List<WorkRecord> workList,

        @JsonProperty("abilityList")
        @Schema(description = "핵심 능력 목록")
        List<AbilityRecord> abilityList,

        @JsonProperty("departList")
        @Schema(description = "관련 학과 목록")
        List<DepartmentRecord> departList,

        @JsonProperty("certiList")
        @Schema(description = "관련 자격증 목록")
        List<CertificateRecord> certiList,

        @JsonProperty("aptitudeList")
        @Schema(description = "적성 목록")
        List<AptitudeRecord> aptitudeList,

        @JsonProperty("interestList")
        @Schema(description = "흥미 목록")
        List<InterestRecord> interestList,

        @JsonProperty("tagList")
        @Schema(description = "관련 태그 목록")
        List<TagRecord> tagList,

        @JsonProperty("researchList")
        @Schema(description = "진로 탐색 활동 목록")
        List<ResearchRecord> researchList,

        @JsonProperty("relVideoList")
        @Schema(description = "관련 동영상 목록")
        List<RelatedVideoRecord> relVideoList,

        @JsonProperty("relSolList")
        @Schema(description = "관련 진로 상담 사례 목록")
        List<RelatedCounselingRecord> relSolList,

        @JsonProperty("jobReadyList")
        @Schema(description = "준비 방법 목록")
        List<JobReadyRecord> jobReadyList,

        @JsonProperty("jobRelOrgList")
        @Schema(description = "관련 기관 목록")
        List<RelatedOrganizationRecord> jobRelOrgList,

        @JsonProperty("forecastList")
        @Schema(description = "직업 전망 목록")
        List<ForecastRecord> forecastList,

        @JsonProperty("eduChart")
        @Schema(description = "학력 분포 차트 데이터")
        List<EducationChartRecord> eduChart,

        @JsonProperty("majorChart")
        @Schema(description = "전공 분포 차트 데이터")
        List<MajorChartRecord> majorChart,

        @JsonProperty("indicatorChart")
        @Schema(description = "직업 지표 차트 데이터")
        List<IndicatorChartRecord> indicatorChart,

        @JsonProperty("performList")
        @Schema(description = "업무 수행능력, 지식, 환경 목록")
        List<PerformanceItemRecord> performList
) {}
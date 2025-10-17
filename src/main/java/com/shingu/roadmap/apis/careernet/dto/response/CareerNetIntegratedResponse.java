package com.shingu.roadmap.apis.careernet.dto.response;

import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common.EnrichedCounselingCase;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.JobEncyclopediaDetailResponse;
import com.shingu.roadmap.apis.careernet.dto.response.info.JobInfoDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * 커리어넷 통합 조회 응답 DTO
 * feature.md의 4단계 프로세스 결과를 통합
 */
@Schema(description = "커리어넷 통합 조회 응답")
@Builder(toBuilder = true)
public record CareerNetIntegratedResponse(
        @Schema(description = "NCS 코드")
        String ncsCode,

        @Schema(description = "NCS 직무명")
        String ncsName,

        @Schema(description = "검색에 사용된 코드")
        SearchCodes searchCodes,

        @Schema(description = "직업 백과 상세 정보")
        JobEncyclopediaDetailResponse encyclopediaDetail,

        @Schema(description = "직업 정보 상세")
        JobInfoDetailResponse jobInfoDetail,

        @Schema(description = "관련 진로 상담 사례 목록 (요약 + 상세)")
        List<EnrichedCounselingCase> counselingCases,

        @Schema(description = "처리 메타데이터")
        ProcessingMetadata metadata
) {

    @Schema(description = "검색에 사용된 코드")
    @Builder
    public record SearchCodes(
            @Schema(description = "직업 백과 테마 코드")
            String encyclopediaThemeCode,

            @Schema(description = "직업 백과 적성 유형 코드")
            String encyclopediaAptitudeCode,

            @Schema(description = "직업 정보 카테고리 코드")
            String jobInfoCategoryCode,

            @Schema(description = "진로 상담 구분 코드")
            String counselingGubunCode
    ) {}

    @Schema(description = "처리 메타데이터")
    @Builder
    public record ProcessingMetadata(
            @Schema(description = "전체 처리 시간 (ms)")
            Long totalProcessingTimeMs,

            @Schema(description = "각 단계 처리 시간")
            StepTimings stepTimings,

            @Schema(description = "처리 성공 여부")
            Boolean success,

            @Schema(description = "오류 메시지 (실패 시)")
            String errorMessage,

            @Schema(description = "경고 메시지 목록")
            List<String> warnings
    ) {}

    @Schema(description = "각 단계 처리 시간")
    @Builder
    public record StepTimings(
            @Schema(description = "1단계: 검색 코드 생성 (ms)")
            Long step1CodeGenerationMs,

            @Schema(description = "2단계: 직업 백과 조회 (ms)")
            Long step2EncyclopediaMs,

            @Schema(description = "3단계: 직업 정보 조회 (ms)")
            Long step3JobInfoMs,

            @Schema(description = "4단계: 진로 상담 사례 목록 조회 (ms)")
            Long step4CounselingListMs,

            @Schema(description = "5단계: 진로 상담 사례 상세 조회 (ms)")
            Long step5CounselingDetailsMs
    ) {}
}

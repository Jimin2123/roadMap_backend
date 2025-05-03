package com.shingu.roadmap.apis.ncs.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "NCS 직무 응답 DTO")
public record NcsOccupationResponse(
        @Schema(description = "직무 리스트")
        List<NcsOccupationItem> data,

        @Schema(description = "응답 정보")
        NcsOccupationDataInfo dataInfo
) {
        @Schema(description = "NCS 직무 항목")
        public record NcsOccupationItem(
                @Schema(description = "직무 ID", example = "01010101")
                String dutyCd,

                @Schema(description = "직무명", example = "공적개발원조사업관리")
                String dutyNm,

                @Schema(description = "직무서비스코드", example = "SVC201600263")
                String dutySvcNo,

                @Schema(description = "직무 설명")
                String dutyDef
        ) { }

        @Schema(description = "NCS 직무정보 응답 정보")
        public record NcsOccupationDataInfo(
                @Schema(description = "응답 코드")
                String code,

                @Schema(description = "응답 메시지")
                String message,

                @Schema(description = "전체 페이지 수")
                int totalPage,

                @Schema(description = "현재 페이지 번호")
                int pageNo,

                @Schema(description = "전체 건수")
                int totCnt
        ) {}
}




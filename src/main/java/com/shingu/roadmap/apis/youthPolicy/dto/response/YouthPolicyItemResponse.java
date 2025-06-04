package com.shingu.roadmap.apis.youthPolicy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정책 Item 응답 DTO")
public record YouthPolicyItemResponse(
        @Schema(description = "정책 번호")
        String plcyNo,

        @Schema(description = "기본 계획 주기")
        String bscPlanCycl,

        @Schema(description = "기본 계획 정책 방식 번호")
        String bscPlanPlcyWayNo,

        @Schema(description = "기본 계획 초점 평가 번호")
        String bscPlanFcsAsmtNo,

        @Schema(description = "기본 계획 평가 번호")
        String bscPlanAsmtNo,

        @Schema(description = "제공 기관 그룹 코드")
        String pvsnInstGroupCd,

        @Schema(description = "정책 제공 방법 코드")
        String plcyPvsnMthdCd,

        @Schema(description = "정책 승인 상태 코드")
        String plcyAprvSttsCd,

        @Schema(description = "정책 이름")
        String plcyNm,

        @Schema(description = "정책 키워드")
        String plcyKywdNm,

        @Schema(description = "정책 설명")
        String plcyExplnCn,

        @Schema(description = "대분류명")
        String lclsfNm,

        @Schema(description = "중분류명")
        String mclsfNm,

        @Schema(description = "정책 지원 내용")
        String plcySprtCn,

        @Schema(description = "감독 기관 코드")
        String sprvsnInstCd,

        @Schema(description = "감독 기관 명")
        String sprvsnInstCdNm,

        @Schema(description = "감독자 이름")
        String sprvsnInstPicNm,

        @Schema(description = "운영 기관 코드")
        String operInstCd,

        @Schema(description = "운영 기관 명")
        String operInstCdNm,

        @Schema(description = "운영자 이름")
        String operInstPicNm,

        @Schema(description = "지원 사회 제한 여부")
        String sprtSclLmtYn,

        @Schema(description = "신청 기간 구분 코드")
        String aplyPrdSeCd,

        @Schema(description = "사업 기간 구분 코드")
        String bizPrdSeCd,

        @Schema(description = "사업 시작일")
        String bizPrdBgngYmd,

        @Schema(description = "사업 종료일")
        String bizPrdEndYmd,

        @Schema(description = "사업 기타 내용")
        String bizPrdEtcCn,

        @Schema(description = "신청 방법 내용")
        String plcyAplyMthdCn,

        @Schema(description = "선정 방법 내용")
        String srngMthdCn,

        @Schema(description = "신청 URL 주소")
        String aplyUrlAddr,

        @Schema(description = "제출 서류 내용")
        String sbmsnDcmntCn,

        @Schema(description = "기타 사항")
        String etcMttrCn,

        @Schema(description = "참고 URL1")
        String refUrlAddr1,

        @Schema(description = "참고 URL2")
        String refUrlAddr2,

        @Schema(description = "지원 사회 수")
        String sprtSclCnt,

        @Schema(description = "도착 순서 지원 여부")
        String sprtArvlSeqYn,

        @Schema(description = "지원 대상 최소 연령")
        String sprtTrgtMinAge,

        @Schema(description = "지원 대상 최대 연령")
        String sprtTrgtMaxAge,

        @Schema(description = "지원 대상 연령 제한 여부")
        String sprtTrgtAgeLmtYn,

        @Schema(description = "혼인 상태 코드")
        String mrgSttsCd,

        @Schema(description = "소득 조건 코드")
        String earnCndSeCd,

        @Schema(description = "최소 소득 금액")
        String earnMinAmt,

        @Schema(description = "최대 소득 금액")
        String earnMaxAmt,

        @Schema(description = "소득 기타 내용")
        String earnEtcCn,

        @Schema(description = "추가 신청 자격 조건")
        String addAplyQlfcCndCn,

        @Schema(description = "참여 대상 조건")
        String ptcpPrpTrgtCn,

        @Schema(description = "조회 수")
        String inqCnt,

        @Schema(description = "등록 기관 코드")
        String rgtrInstCd,

        @Schema(description = "등록 기관 명")
        String rgtrInstCdNm,

        @Schema(description = "상위 기관 코드")
        String rgtrUpInstCd,

        @Schema(description = "상위 기관 명")
        String rgtrUpInstCdNm,

        @Schema(description = "최고 상위 기관 코드")
        String rgtrHghrkInstCd,

        @Schema(description = "최고 상위 기관 명")
        String rgtrHghrkInstCdNm,

        @Schema(description = "우편번호")
        String zipCd,

        @Schema(description = "정책 주요 코드")
        String plcyMajorCd,

        @Schema(description = "직업 코드")
        String jobCd,

        @Schema(description = "학교 코드")
        String schoolCd,

        @Schema(description = "신청 일자")
        String aplyYmd,

        @Schema(description = "최초 등록 일자")
        String frstRegDt,

        @Schema(description = "최종 수정 일자")
        String lastMdfcnDt,

        @Schema(description = "세부사업 코드")
        String sbizCd
) {}
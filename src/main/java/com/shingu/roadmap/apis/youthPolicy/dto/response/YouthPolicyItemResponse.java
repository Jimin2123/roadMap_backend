package com.shingu.roadmap.apis.youthPolicy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정책 Item 응답 DTO")
public record YouthPolicyItemResponse(
        @Schema(description = "정책 번호")
        String plcyNo,

        @Schema(description = "기본 계획 차수")
        String bscPlanCycl,

        @Schema(description = "기본 계획 정책 방향 번호")
        String bscPlanPlcyWayNo,

        @Schema(description = "기본 계획 중점 과제 번호")
        String bscPlanFcsAsmtNo,

        @Schema(description = "기본 계획 과제 번호")
        String bscPlanAsmtNo,

        @Schema(description = "제공 기관 그룹 코드")
        String pvsnInstGroupCd,

        @Schema(description = "정책 제공 방법 코드")
        String plcyPvsnMthdCd,

        @Schema(description = "정책 승인 상태 코드")
        String plcyAprvSttsCd,

        @Schema(description = "정책명")
        String plcyNm,

        @Schema(description = "정책 키워드명")
        String plcyKywdNm,

        @Schema(description = "정책 설명 내용")
        String plcyExplnCn,

        @Schema(description = "정책 대분류명")
        String lclsfNm,

        @Schema(description = "정책 중분류명")
        String mclsfNm,

        @Schema(description = "정책 지원 내용")
        String plcySprtCn,

        @Schema(description = "주관 기관 코드")
        String sprvsnInstCd,

        @Schema(description = "주관 기관 코드명")
        String sprvsnInstCdNm,

        @Schema(description = "주관 기관 담당자명")
        String sprvsnInstPicNm,

        @Schema(description = "운영 기관 코드")
        String operInstCd,

        @Schema(description = "운영 기관 코드명")
        String operInstCdNm,

        @Schema(description = "운영 기관 담당자명")
        String operInstPicNm,

        @Schema(description = "지원 규모 제한 여부")
        String sprtSclLmtYn,

        @Schema(description = "신청 기간 구분 코드")
        String aplyPrdSeCd,

        @Schema(description = "사업 기간 구분 코드")
        String bizPrdSeCd,

        @Schema(description = "사업 기간 시작 일자")
        String bizPrdBgngYmd,

        @Schema(description = "사업 기간 종료 일자")
        String bizPrdEndYmd,

        @Schema(description = "사업 기간 기타 내용")
        String bizPrdEtcCn,

        @Schema(description = "정책 신청 방법 내용")
        String plcyAplyMthdCn,

        @Schema(description = "심사 방법 내용")
        String srngMthdCn,

        @Schema(description = "신청 URL 주소")
        String aplyUrlAddr,

        @Schema(description = "제출 서류 내용")
        String sbmsnDcmntCn,

        @Schema(description = "기타 사항 내용")
        String etcMttrCn,

        @Schema(description = "참고 URL 주소")
        String refUrlAddr1,

        @Schema(description = "참고 URL 주소")
        String refUrlAddr2,

        @Schema(description = "지원 규모 수")
        String sprtSclCnt,

        @Schema(description = "지원 도착 순서 여부")
        String sprtArvlSeqYn,

        @Schema(description = "지원 대상 최소 연령")
        String sprtTrgtMinAge,

        @Schema(description = "지원 대상 최대 연령")
        String sprtTrgtMaxAge,

        @Schema(description = "지원 대상 연령 제한 여부")
        String sprtTrgtAgeLmtYn,

        @Schema(description = "결혼 상태 코드")
        String mrgSttsCd,

        @Schema(description = "소득 조건 구분 코드")
        String earnCndSeCd,

        @Schema(description = "소득 최소 금액")
        String earnMinAmt,

        @Schema(description = "소득 최대 금액")
        String earnMaxAmt,

        @Schema(description = "소득 기타 내용")
        String earnEtcCn,

        @Schema(description = "추가 신청 자격 조건 내용")
        String addAplyQlfcCndCn,

        @Schema(description = "참여 제안 대상 내용")
        String ptcpPrpTrgtCn,

        @Schema(description = "조회수")
        String inqCnt,

        @Schema(description = "등록자 기관 코드")
        String rgtrInstCd,

        @Schema(description = "등록자 기관 코드명")
        String rgtrInstCdNm,

        @Schema(description = "등록자 상위 기관 코드")
        String rgtrUpInstCd,

        @Schema(description = "등록자 상위 기관 코드명")
        String rgtrUpInstCdNm,

        @Schema(description = "등록자 최상위 기관 코드")
        String rgtrHghrkInstCd,

        @Schema(description = "등록자 최상위 기관 코드명")
        String rgtrHghrkInstCdNm,

        @Schema(description = "정책 거주 지역 코드")
        String zipCd,

        @Schema(description = "정책 전공 요건 코드")
        String plcyMajorCd,

        @Schema(description = "정책 취업 요건 코드")
        String jobCd,

        @Schema(description = "정책 학력 요건 코드")
        String schoolCd,

        @Schema(description = "신청 기간")
        String aplyYmd,

        @Schema(description = "최초 등록 일시")
        String frstRegDt,

        @Schema(description = "최종 수정 일시")
        String lastMdfcnDt,

        @Schema(description = "정책 특화 요건 코드")
        String sBizCd
) {}
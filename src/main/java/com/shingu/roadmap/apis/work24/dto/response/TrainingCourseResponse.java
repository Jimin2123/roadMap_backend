package com.shingu.roadmap.apis.work24.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "워크24 훈련과정 응답 DTO")
public record TrainingCourseResponse(
  @Schema(description = "훈련과정 리스트")
  List<TrainCourseItem> srchList,

  @Schema(description = "검색된 총 건수")
  int scn_cnt,

  @Schema(description = "페이지당 출력개수, 페이지당 표현될 자료의 개수")
  String pageSize,

  @Schema(description = "현재페이지")
  String pageNum
) {
  @Schema(description = "훈련과정 항목")
  public record TrainCourseItem(
          @Schema(description = "고용보험3개월 취업누적인원 10인이하 여부 (Y/N)\n" +
                  "17.11.07부터 제공되지 않는 항목이나 기존 API 사용자를 위해 Null값을 제공")
          String eiEmplCnt3Gt10,

          @Schema(description = "고용보험6개월 취업률")
          String eiEmplRate6,

          @Schema(description = "고용보험3개월 취업인원 수")
          String eiEmplCnt3,

          @Schema(description = "고용보험3개월 취업률")
          String eiEmplRate3,

          @Schema(description = "자격증")
          String certificate,

          @Schema(description = "제목")
          String title,

          @Schema(description = "실제 훈련비")
          String realMan,

          @Schema(description = "전화번호")
          String telNo,

          @Schema(description = "만족도 점수")
          String stdgScor,

          @Schema(description = "훈련시작일자")
          String traStartDate,

          @Schema(description = "등급")
          String grade,

          @Schema(description = "NCS 코드")
          String ncsCd,

          @Schema(description = "수강신청 인원")
          String regCourseMan,

          @Schema(description = "훈련과정 순차")
          String trprDegr,

          @Schema(description = "주소")
          String address,

          @Schema(description = "훈련종료일자")
          String traEndDate,

          @Schema(description = "부 제목")
          String subTitle,

          @Schema(description = "훈련기관 코드")
          String instCd,

          @Schema(description = "지역코드(중분류)")
          String trngAreaCd,

          @Schema(description = "훈련과정ID")
          String trprId,

          @Schema(description = "정원")
          String yardMan,

          @Schema(description = "수강비")
          String courseMan,

          @Schema(description = "훈련대상")
          String trainTarget,

          @Schema(description = "훈련구분")
          String trainTargetCd,

          @Schema(description = "훈련기관ID")
          String trainstCstId,

          @Schema(description = "컨텐츠")
          String contents,

          @Schema(description = "부 제목 링크")
          String subTitleLink,

          @Schema(description = "제목 링크")
          String titleLink,

          @Schema(description = "제목 아이콘")
          String titleIcon
  ) {}
}

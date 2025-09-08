package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "직업 상세 정보 DTO")
@Getter
@Setter
public class JobDetail {

  // 기본 정보
  @Schema(description = "직업명", example = "응용소프트웨어개발자")
  private String job;

  @Schema(description = "핵심 능력", example = "공간시각능력, 수리논리력, 창의력")
  private String ability;

  @Schema(description = "유사 직업명", example = "시스템소프트웨어개발자, 웹개발자")
  private String similarjob;

  @Schema(description = "하는 일")
  private String summary;

  @Schema(description = "적성 및 흥미")
  private String aptitude;

  // 관련 학과/자격
  @Schema(description = "관련 자격증 (텍스트)", example = "정보처리기사, 정보보안기사")
  private String relatedCertifications; // <capacity>

  @Schema(description = "관련 학과 목록")
  private List<Major> relatedMajors; // <major>

  // 분류
  @Schema(description = "커리어넷 직업 분류", example = "공학계열")
  private String cnetJobDvs;

  @Schema(description = "표준 직업 분류", example = "2221")
  private String stdCodeNm;

  @Schema(description = "고용 직업 분류", example = "3121")
  private String emplymCodeNm;

  // 취업 현황
  @Schema(description = "입직 및 취업 방법")
  private String empway;

  @Schema(description = "고용 현황")
  private String employment;

  @Schema(description = "임금 수준")
  private String salery;

  // 준비 방법
  @Schema(description = "정규 교육 과정")
  private String preparation;

  @Schema(description = "직업 훈련")
  private String training;

  @Schema(description = "관련 자격증 (상세)")
  private String certification;

  // 문의 기관
  @Schema(description = "문의 기관 목록")
  private List<ContactInstitute> contact;

  // 직업 전망
  @Schema(description = "전망 요약", example = "향후 10년간 일자리가 증가할 것으로 전망됨")
  private String prospect; // <job_possibility>의 <possibility>

  @Schema(description = "전망 차트 데이터")
  private List<ChartItem> prospectChart; // <job_possibility>의 <chart_item_list>

  // 특성 정보
  @Schema(description = "특성(성별 비율)")
  private CharacteristicData<CharacteristicItem> genderRatio; // GenCD

  @Schema(description = "특성(학교급별 비율)")
  private CharacteristicData<CharacteristicItem> schoolLevelRatio; // SchClass

  @Schema(description = "특성(중학생 적성 유형)")
  private CharacteristicData<CharacteristicRank> middleSchoolAptitude; // lstMiddleAptd

  @Schema(description = "특성(고등학생 적성 유형)")
  private CharacteristicData<CharacteristicRank> highSchoolAptitude; // lstHighAptd

  @Schema(description = "특성(선호 직업 가치)")
  private CharacteristicData<CharacteristicRank> jobValues; // lstVals
}
package com.shingu.roadmap.apis.saramin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사람인 채용 정보 전체 응답")
public record SaraminJobListResponse(
        @Schema(description = "채용 정보 목록 데이터")
        Jobs jobs
) {

  @Schema(description = "채용 정보 목록 객체")
  public record Jobs(

          @Schema(description = "채용 정보 총 개수")
          int count,

          @Schema(description = "시작 위치")
          int start,

          @Schema(description = "전체 채용 공고 수 (문자열로 반환됨)")
          String total,

          @Schema(description = "채용 정보 목록")
          List<Job> job

  ) {

    @Schema(description = "채용 정보 상세 DTO")
    public record Job(

            @Schema(description = "공고 ID")
            String id,

            @Schema(description = "채용공고 상세 URL")
            String url,

            @Schema(description = "공고 활성 상태 (1: 활성화, 0: 비활성화)")
            int active,

            @JsonProperty("posting-timestamp")
            @Schema(description = "공고 등록 시각 (timestamp)")
            String postingTimestamp,

            @Schema(description = "회사 정보")
            Company company,

            @Schema(description = "직무 및 조건 정보")
            Position position,

            @Schema(description = "공고 키워드")
            String keyword,

            @Schema(description = "급여 정보")
            Salary salary,

            @JsonProperty("modification-timestamp")
            @Schema(description = "수정 시각")
            String modificationTimestamp,

            @JsonProperty("opening-timestamp")
            @Schema(description = "공고 오픈 시각")
            String openingTimestamp,

            @JsonProperty("expiration-timestamp")
            @Schema(description = "마감 시각")
            String expirationTimestamp,

            @JsonProperty("close-type")
            @Schema(description = "마감 방식")
            CloseType closeType

    ) {
      @Schema(description = "회사 정보 DTO")
      public record Company(
              @Schema(description = "회사 상세 정보")
              Detail detail
      ) {
        public record Detail(
                @Schema(description = "회사 상세 페이지 URL") String href,
                @Schema(description = "회사명") String name,
                @Schema(description = "회사 로고 이미지 URL") String logoUrl
        ) {}
      }

      @Schema(description = "직무 및 위치 정보 DTO")
      public record Position(

              @Schema(description = "공고 제목") String title,
              @Schema(description = "산업군 정보") Industry industry,
              @Schema(description = "지역 정보") Location location,

              @JsonProperty("job-type")
              @Schema(description = "고용형태 정보") JobType jobType,

              @JsonProperty("job-mid-code")
              @Schema(description = "직무 중분류") JobMidCode jobMidCode,

              @JsonProperty("job-code")
              @Schema(description = "직무 소분류") JobCode jobCode,

              @JsonProperty("experience-level")
              @Schema(description = "경력 정보") ExperienceLevel experienceLevel,

              @JsonProperty("required-education-level")
              @Schema(description = "학력 요구사항") RequiredEducationLevel requiredEducationLevel

      ) {
        public record Industry(String code, String name) {}
        public record Location(String code, String name) {}
        public record JobType(String code, String name) {}
        public record JobMidCode(String code, String name) {}
        public record JobCode(String code, String name) {}
        public record ExperienceLevel(int code, int min, int max, String name) {}
        public record RequiredEducationLevel(String code, String name) {}
      }

      @Schema(description = "급여 정보 DTO")
      public record Salary(String code, String name) {}

      @Schema(description = "마감 방식 정보 DTO")
      public record CloseType(String code, String name) {}
    }
  }
}
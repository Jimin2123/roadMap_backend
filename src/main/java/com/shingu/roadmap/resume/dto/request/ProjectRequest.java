package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.Set;

@Schema(description = "프로젝트 등록 및 수정 요청 DTO")
public record ProjectRequest(
        @Schema(description = "프로젝트 이름", example = "개인 프로젝트: 쇼핑몰 웹사이트")
        @NotBlank(message = "프로젝트 이름은 필수입니다.")
        @Size(max = 100, message = "프로젝트 이름은 100자를 초과할 수 없습니다.")
        String name,

        @Schema(description = "프로젝트 설명", example = "Spring Boot와 React를 이용한 쇼핑몰 웹사이트 개발")
        @Size(max = 1000, message = "프로젝트 설명은 1000자를 초과할 수 없습니다.")
        String description,

        @Schema(description = "프로젝트에서 맡은 역할", example = "백엔드 개발")
        @Size(max = 100, message = "역할은 100자를 초과할 수 없습니다.")
        String role,

        @Schema(description = "프로젝트 기간", example = "{\"startDate\":\"2023-01-01\",\"endDate\":\"2023-06-01\"}")
        @Valid
        PeriodRequest period,

        @Schema(description = "프로젝트 관련 URL", example = "https://github.com/your-repo")
        @URL(message = "올바른 URL 형식이어야 합니다.")
        String url,

        @Schema(description = "주요 성과", example = "[\"로그인 API 응답 시간 50% 단축\", \"Docker 기반 배포 자동화\"]")
        @Size(max = 10, message = "주요 성과는 최대 10개까지 입력 가능합니다.")
        List<@Size(max = 200, message = "각 성과는 200자를 초과할 수 없습니다.") String> achievements,

        @Schema(description = "사용 기술", example = "[\"Java\", \"Spring Boot\", \"React\"]")
        @Size(max = 20, message = "사용 기술은 최대 20개까지 입력 가능합니다.")
        Set<@NotBlank(message = "기술명은 공백일 수 없습니다.") @Size(max = 50, message = "기술명은 50자를 초과할 수 없습니다.") String> techStack
) {
}
package com.shingu.roadmap.resume.dto.request;

import com.shingu.roadmap.resume.domain.Period;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

@Schema(description = "프로젝트 등록 및 수정 요청 DTO")
public record ProjectRequest(
        @Schema(description = "프로젝트 이름", example = "개인 프로젝트: 쇼핑몰 웹사이트")
        String name,

        @Schema(description = "프로젝트 설명", example = "Spring Boot와 React를 이용한 쇼핑몰 웹사이트 개발")
        String description,

        @Schema(description = "프로젝트에서 맡은 역할", example = "백엔드 개발") // 추가
        String role,

        @Schema(description = "프로젝트 기간", example = "{\"startDate\":\"2023-01-01\",\"endDate\":\"2023-06-01\"}")
        PeriodRequest period,

        @Schema(description = "프로젝트 관련 URL", example = "https://github.com/your-repo")
        String url,

        @Schema(description = "주요 성과", example = "[\"로그인 API 응답 시간 50% 단축\", \"Docker 기반 배포 자동화\"]") // 추가
        List<String> achievements,

        @Schema(description = "사용 기술", example = "[\"Java\", \"Spring Boot\", \"React\"]")
        Set<String> techStack
) {
}
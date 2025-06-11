package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "프로젝트 등록 요청 DTO")
public record ProjectRequest(
        @Schema(description = "프로젝트 제목", example = "개인 프로젝트: 쇼핑몰 웹사이트")
        String title,

        @Schema(description = "프로젝트 설명", example = "Spring Boot와 React를 이용한 쇼핑몰 웹사이트 개발")
        String description,

        @Schema(description = "프로젝트 기간", example = "2022-01 ~ 2022-06")
        String period,

        @Schema(description = "사용 기술", example = "[\"Java\", \"Spring Boot\", \"React\"]")
        Set<String> techStack
) { }

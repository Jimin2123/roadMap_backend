package com.shingu.roadmap.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "커리어넷 직업 정보 (진단 결과 보강용)")
public record CareerNetJobInfo(
        @Schema(description = "직업명", example = "응용소프트웨어개발자")
        String jobName,

        @Schema(description = "하는 일 (업무 요약)")
        String summary,

        @Schema(description = "핵심 능력", example = "공간시각능력, 수리논리력, 창의력")
        String coreAbilities,

        @Schema(description = "적성 및 흥미")
        String aptitudeAndInterest,

        @Schema(description = "유사 직업명", example = "시스템소프트웨어개발자, 웹개발자")
        String similarJobs,

        @Schema(description = "관련 자격증", example = "정보처리기사, 정보보안기사")
        String relatedCertifications,

        @Schema(description = "관련 학과 목록", example = "[\"컴퓨터공학\", \"소프트웨어학\"]")
        List<String> relatedMajors,

        @Schema(description = "입직 및 취업 방법")
        String employmentMethod,

        @Schema(description = "고용 현황")
        String employmentStatus,

        @Schema(description = "임금 수준")
        String salaryLevel,

        @Schema(description = "직업 전망 요약", example = "향후 10년간 일자리가 증가할 것으로 전망됨")
        String prospect,

        @Schema(description = "정규 교육 과정")
        String educationPath,

        @Schema(description = "직업 훈련 정보")
        String trainingInfo
) {
}

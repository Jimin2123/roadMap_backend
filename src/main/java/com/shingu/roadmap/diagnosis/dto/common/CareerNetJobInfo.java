package com.shingu.roadmap.diagnosis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "커리어넷 직업 정보 (진단 결과 보강용)")
public record CareerNetJobInfo(
        @Schema(
                description = "직업명",
                example = "응용소프트웨어개발자",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String jobName,

        @Schema(
                description = "하는 일 (업무 요약)",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String summary,

        @Schema(
                description = "핵심 능력",
                example = "공간시각능력, 수리논리력, 창의력",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String coreAbilities,

        @Schema(
                description = "적성 및 흥미",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String aptitudeAndInterest,

        @Schema(
                description = "유사 직업명",
                example = "시스템소프트웨어개발자, 웹개발자",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String similarJobs,

        @Schema(
                description = "관련 자격증",
                example = "정보처리기사, 정보보안기사",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String relatedCertifications,

        @Schema(
                description = "관련 학과 목록",
                example = "[\"컴퓨터공학\", \"소프트웨어학\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        List<String> relatedMajors,

        @Schema(
                description = "입직 및 취업 방법",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String employmentMethod,

        @Schema(
                description = "고용 현황",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String employmentStatus,

        @Schema(
                description = "임금 수준",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String salaryLevel,

        @Schema(
                description = "직업 전망 요약",
                example = "향후 10년간 일자리가 증가할 것으로 전망됨",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String prospect,

        @Schema(
                description = "정규 교육 과정",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String educationPath,

        @Schema(
                description = "직업 훈련 정보",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String trainingInfo
) {
}

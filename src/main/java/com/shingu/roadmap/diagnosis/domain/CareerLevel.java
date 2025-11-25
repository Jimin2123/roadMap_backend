package com.shingu.roadmap.diagnosis.domain;

/**
 * 경력 레벨 분류
 *
 * AI 기반 경력 레벨 진단에 사용되는 레벨 분류입니다.
 * 단순 연수가 아닌 프로젝트 경험, 기술 깊이, 리더십 등을 종합 평가하여 결정됩니다.
 */
public enum CareerLevel {
    /**
     * 주니어 (Junior)
     * - 경력: 0-3년
     * - 특징: 지도 하에 작업 수행, 기본 기술 활용
     */
    JUNIOR("주니어", "0-3년 경력, 기본 업무 수행"),

    /**
     * 미드 레벨 (Mid-level)
     * - 경력: 3-7년
     * - 특징: 독립적 업무 수행, 중급 기술 활용, 일부 멘토링
     */
    MID("미드레벨", "3-7년 경력, 독립적 업무 수행"),

    /**
     * 시니어 (Senior)
     * - 경력: 7-12년
     * - 특징: 전문가 수준, 아키텍처 설계, 팀 리딩, 멘토링
     */
    SENIOR("시니어", "7-12년 경력, 전문가 수준, 리더십"),

    /**
     * 리드 (Lead/Principal)
     * - 경력: 12년 이상
     * - 특징: 기술 의사결정, 전사 아키텍처, 다수 팀 리딩
     */
    LEAD("리드", "12년 이상 경력, 기술 리더십, 전략적 의사결정");

    private final String displayName;
    private final String description;

    CareerLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 경력 연수를 기반으로 기본 레벨을 추정합니다.
     * 이는 fallback 용도이며, AI 평가가 우선됩니다.
     *
     * @param careerYears 총 경력 연수
     * @return 추정된 경력 레벨
     */
    public static CareerLevel fromCareerYears(double careerYears) {
        if (careerYears < 3.0) {
            return JUNIOR;
        } else if (careerYears < 7.0) {
            return MID;
        } else if (careerYears < 12.0) {
            return SENIOR;
        } else {
            return LEAD;
        }
    }
}

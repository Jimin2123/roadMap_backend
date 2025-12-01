package com.shingu.roadmap.apis.openai.util;

import com.shingu.roadmap.resume.domain.Period;

/**
 * Period 포맷팅 유틸리티
 *
 * Period 객체를 사람이 읽기 쉬운 문자열로 변환합니다.
 */
public final class PeriodFormatter {

    private PeriodFormatter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Period를 사람이 읽기 쉬운 문자열로 변환합니다.
     *
     * 예시:
     * - "2020-01-01 ~ 2022-12-31"
     * - "2021-06-01 ~ 재직중"
     * - "미정 ~ 재직중"
     *
     * @param period 변환할 Period 객체
     * @return 포맷팅된 문자열
     */
    public static String format(Period period) {
        if (period == null) {
            return "";
        }

        String startStr = period.getStartDate() != null ?
                period.getStartDate().toString() : "미정";
        String endStr = period.getEndDate() != null ?
                period.getEndDate().toString() : "재직중";

        return startStr + " ~ " + endStr;
    }
}

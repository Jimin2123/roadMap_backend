package com.shingu.roadmap.apis.openai.util;

import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.resume.domain.Career;
import com.shingu.roadmap.resume.domain.Period;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 경력 계산 유틸리티
 *
 * 사용자의 경력 정보를 분석하고 총 경력 연수를 계산합니다.
 * 겹치는 경력 기간을 병합하여 중복을 제거합니다.
 */
@Slf4j
public final class CareerCalculator {

    private CareerCalculator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 사용자의 총 경력 연수를 계산합니다.
     *
     * 계산 방식:
     * 1. Resume에서 모든 Career 정보를 가져옴
     * 2. 각 Career의 Period(시작일~종료일)를 기반으로 근무 기간 계산
     * 3. 종료일이 없는 경우(재직중) 현재 날짜를 종료일로 사용
     * 4. **겹치는 경력 기간을 병합하여 중복 제거 (Merge Overlapping Intervals)**
     * 5. 병합된 기간들의 총합을 계산하여 반환
     *
     * @param profile 사용자 프로필
     * @return 총 경력 연수 (년 단위, 소수점)
     */
    public static double calculateTotalCareerYears(Profile profile) {
        if (profile.getResume() == null || profile.getResume().getCareers() == null
                || profile.getResume().getCareers().isEmpty()) {
            log.debug("[CareerCalculator] No career information found");
            return 0.0;
        }

        List<Career> careers = profile.getResume().getCareers();

        // 1. 유효한 Career Period만 수집
        List<Period> periods = new ArrayList<>();
        for (Career career : careers) {
            if (career.getPeriod() == null || career.getPeriod().getStartDate() == null) {
                log.debug("[CareerCalculator] Career period is null or has no start date, skipping");
                continue;
            }

            LocalDate startDate = career.getPeriod().getStartDate();
            LocalDate endDate = career.getPeriod().getEndDate();

            // 재직중인 경우 현재 날짜를 종료일로 사용
            if (endDate == null) {
                endDate = LocalDate.now();
                log.debug("[CareerCalculator] Career is ongoing, using current date as end date");
            }

            periods.add(Period.of(startDate, endDate));
        }

        if (periods.isEmpty()) {
            log.debug("[CareerCalculator] No valid career periods found");
            return 0.0;
        }

        // 2. 겹치는 기간 병합
        List<Period> mergedPeriods = mergeOverlappingPeriods(periods);

        // 3. 병합된 기간들의 총 연수 계산
        double totalYears = 0.0;
        for (Period period : mergedPeriods) {
            long daysBetween = ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate());
            double years = daysBetween / 365.0;
            totalYears += years;

            log.debug("[CareerCalculator] Merged period: {} ~ {} = {} years",
                    period.getStartDate(), period.getEndDate(), String.format("%.2f", years));
        }

        log.info("[CareerCalculator] Total career calculated (after merging overlaps): {} years (from {} merged periods)",
                String.format("%.2f", totalYears), mergedPeriods.size());
        return totalYears;
    }

    /**
     * 겹치는 경력 기간을 병합합니다 (Merge Overlapping Intervals Algorithm).
     *
     * 알고리즘:
     * 1. 시작일 기준으로 정렬
     * 2. 현재 구간과 다음 구간이 겹치는지 확인
     * 3. 겹치면 병합, 안 겹치면 현재 구간을 결과에 추가하고 다음으로 이동
     *
     * 예시:
     * - Input: [(2020-01-01, 2022-12-31), (2021-06-01, 2023-06-30)]
     * - Output: [(2020-01-01, 2023-06-30)]
     *
     * @param periods 경력 기간 리스트
     * @return 병합된 경력 기간 리스트
     */
    public static List<Period> mergeOverlappingPeriods(List<Period> periods) {
        if (periods.size() <= 1) {
            return new ArrayList<>(periods);
        }

        // 1. 시작일 기준 정렬
        List<Period> sorted = new ArrayList<>(periods);
        sorted.sort(Comparator.comparing(Period::getStartDate));

        log.debug("[CareerCalculator] Merging {} career periods", sorted.size());

        // 2. 겹치는 구간 병합
        List<Period> merged = new ArrayList<>();
        Period current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            Period next = sorted.get(i);

            // 겹치는지 확인: 현재 종료일 >= 다음 시작일
            if (current.getEndDate().isAfter(next.getStartDate()) ||
                    current.getEndDate().isEqual(next.getStartDate())) {
                // 겹침: 병합 (종료일을 더 늦은 날짜로)
                LocalDate mergedEndDate = current.getEndDate().isAfter(next.getEndDate())
                        ? current.getEndDate()
                        : next.getEndDate();
                current = Period.of(current.getStartDate(), mergedEndDate);
                log.debug("[CareerCalculator] Merged overlapping periods: {} ~ {}",
                        current.getStartDate(), current.getEndDate());
            } else {
                // 겹치지 않음: 현재 구간을 결과에 추가하고 다음으로 이동
                merged.add(current);
                current = next;
            }
        }

        // 마지막 구간 추가
        merged.add(current);

        log.debug("[CareerCalculator] Merged {} periods into {} non-overlapping periods",
                sorted.size(), merged.size());

        return merged;
    }
}

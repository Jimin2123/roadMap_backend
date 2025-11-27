package com.shingu.roadmap.apis.openai.util;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 채용공고 매칭 유틸리티
 *
 * 채용공고의 요구사항(학력, 급여, 경력)과 사용자 정보를 비교하여
 * 적합성을 판단합니다.
 */
@Slf4j
public final class JobMatchingUtil {

    private JobMatchingUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 채용공고가 사용자의 학력 수준에 적합한지 판단합니다.
     *
     * 판단 기준:
     * 1. 채용공고의 requiredEducationLevel 필드 분석
     * 2. "학력무관": 모든 사용자에게 적합
     * 3. 사용자 학력이 요구사항보다 높거나 같으면 적합
     * 4. 학력 정보가 없는 경우: 보수적으로 적합하다고 판단
     *
     * @param job 채용공고
     * @param userEducationLevel 사용자 학력 (예: "대학교졸업(4년)")
     * @return 적합 여부
     */
    public static boolean isJobSuitableForEducationLevel(
            SaraminJobListResponse.Jobs.Job job,
            String userEducationLevel) {

        if (job.position() == null || job.position().requiredEducationLevel() == null
                || job.position().requiredEducationLevel().name() == null) {
            // 학력 정보가 없으면 보수적으로 적합하다고 판단
            return true;
        }

        String requiredEducation = job.position().requiredEducationLevel().name();
        log.debug("[JobMatchingUtil] Checking job {} - required education: '{}', user education: '{}'",
                job.id(), requiredEducation, userEducationLevel);

        // 1. 학력무관
        if (requiredEducation.contains("학력무관")) {
            return true;
        }

        // 2. 사용자 학력이 null인 경우 보수적으로 허용
        if (userEducationLevel == null || userEducationLevel.isBlank()) {
            log.debug("[JobMatchingUtil] User education level is null/blank, allowing job conservatively");
            return true;
        }

        // 3. 학력 레벨 비교
        try {
            int userLevel = getEducationLevelCode(userEducationLevel);
            int requiredLevel = getEducationLevelCode(requiredEducation);

            boolean suitable = userLevel >= requiredLevel;
            log.debug("[JobMatchingUtil] Education comparison - userLevel: {}, requiredLevel: {}, suitable: {}",
                    userLevel, requiredLevel, suitable);
            return suitable;

        } catch (Exception e) {
            // 파싱 실패 시 보수적으로 허용
            log.debug("[JobMatchingUtil] Could not parse education levels, allowing job conservatively - error: {}",
                    e.getMessage());
            return true;
        }
    }

    /**
     * 학력 문자열을 학력 레벨 코드로 변환합니다.
     *
     * 학력 레벨 순서:
     * 0 = 학력무관
     * 1 = 고등학교졸업
     * 2 = 대학졸업(2,3년) / 전문대졸
     * 3 = 대학교졸업(4년) / 대졸
     * 4 = 석사졸업
     * 5 = 박사졸업
     *
     * @param educationText 학력 문자열
     * @return 학력 레벨 코드 (0~5)
     */
    public static int getEducationLevelCode(String educationText) {
        if (educationText == null || educationText.isBlank()) {
            return 0; // 학력무관으로 처리
        }

        String normalized = educationText.trim();

        // 학력무관
        if (normalized.contains("학력무관") || normalized.contains("무관")) {
            return 0;
        }

        // 박사
        if (normalized.contains("박사")) {
            return 5;
        }

        // 석사
        if (normalized.contains("석사")) {
            return 4;
        }

        // 대학교졸업(4년) / 대졸 / 학사
        if (normalized.contains("대학교졸업") || normalized.contains("4년") ||
                normalized.contains("대졸") || normalized.contains("학사")) {
            return 3;
        }

        // 대학졸업(2,3년) / 전문대 / 초대졸
        if (normalized.contains("대학졸업") || normalized.contains("2,3년") || normalized.contains("2~3년") ||
                normalized.contains("전문대") || normalized.contains("초대졸")) {
            return 2;
        }

        // 고등학교졸업 / 고졸
        if (normalized.contains("고등학교") || normalized.contains("고졸")) {
            return 1;
        }

        // 파싱 실패: 기본값 0 (학력무관)으로 처리
        log.debug("[JobMatchingUtil] Unknown education level format: '{}', treating as NO_REQUIREMENT",
                normalized);
        return 0;
    }

    /**
     * 채용공고가 사용자의 희망 최소 급여 조건에 적합한지 판단합니다.
     *
     * 판단 기준:
     * 1. 사용자가 희망 최소 급여를 설정하지 않은 경우 → 모든 공고 허용
     * 2. 채용공고의 급여 정보가 없는 경우 → 보수적으로 허용
     * 3. 급여 정보가 있는 경우 → 공고의 최소 급여가 사용자 희망보다 높거나 같으면 허용
     *
     * @param job 채용공고
     * @param userDesiredMinSalary 사용자 희망 최소 급여 (만원 단위, NULL이면 조건 없음)
     * @return 급여 조건이 적합한 경우 true, 아니면 false
     */
    public static boolean isJobSuitableForSalary(
            SaraminJobListResponse.Jobs.Job job,
            Integer userDesiredMinSalary) {

        // 1. 사용자가 급여 조건을 설정하지 않은 경우 모든 공고 허용
        if (userDesiredMinSalary == null) {
            return true;
        }

        // 2. 채용공고에 급여 정보가 없는 경우 보수적으로 허용
        if (job.salary() == null || job.salary().name() == null || job.salary().name().isBlank()) {
            log.debug("[JobMatchingUtil] Job {} has no salary info, allowing conservatively", job.id());
            return true;
        }

        String salaryText = job.salary().name();

        // 3. 특수 케이스: "회사내규에 따름", "면접 후 결정" 등 → 보수적으로 허용
        if (salaryText.contains("회사내규") || salaryText.contains("면접") ||
                salaryText.contains("추후") || salaryText.contains("협의")) {
            log.debug("[JobMatchingUtil] Job {} has negotiable salary, allowing conservatively", job.id());
            return true;
        }

        // 4. 급여 파싱 및 비교
        try {
            Integer jobMinSalary = parseSalaryMin(salaryText);
            if (jobMinSalary == null) {
                // 파싱 실패 시 보수적으로 허용
                log.debug("[JobMatchingUtil] Could not parse salary '{}', allowing conservatively", salaryText);
                return true;
            }

            boolean suitable = jobMinSalary >= userDesiredMinSalary;
            log.debug("[JobMatchingUtil] Salary comparison - job min: {} 만원, user desired: {} 만원, suitable: {}",
                    jobMinSalary, userDesiredMinSalary, suitable);
            return suitable;

        } catch (Exception e) {
            // 예외 발생 시 보수적으로 허용
            log.debug("[JobMatchingUtil] Error parsing salary '{}': {}, allowing conservatively",
                    salaryText, e.getMessage());
            return true;
        }
    }

    /**
     * 급여 문자열에서 최소 급여를 추출합니다.
     *
     * 예시:
     * - "3000만원~4000만원" → 3000
     * - "연봉 3500만원 이상" → 3500
     * - "4000만원" → 4000
     * - "3000만원 ~ 5000만원" → 3000
     *
     * @param salaryText 급여 문자열
     * @return 최소 급여 (만원 단위), 파싱 실패 시 null
     */
    public static Integer parseSalaryMin(String salaryText) {
        if (salaryText == null || salaryText.isBlank()) {
            return null;
        }

        // 숫자만 추출하는 정규식
        Pattern pattern = Pattern.compile("(\\d{1,5})(?:만원|만|원)");
        Matcher matcher = pattern.matcher(salaryText);

        if (matcher.find()) {
            String firstNumber = matcher.group(1);
            return Integer.parseInt(firstNumber);
        }

        return null;
    }

    /**
     * 채용공고가 사용자의 경력 수준에 적합한지 판단합니다.
     *
     * 판단 기준:
     * 1. 채용공고의 experienceLevel 필드 분석
     * 2. "신입", "경력무관": 모든 사용자에게 적합
     * 3. "경력 X년↑", "경력 X~Y년": 사용자 경력이 요구사항을 충족하는 경우에만 적합
     * 4. 경력 요구사항을 파싱할 수 없는 경우: 보수적으로 적합하다고 판단
     *
     * @param job 채용공고
     * @param userCareerYears 사용자 총 경력 연수
     * @return 적합 여부
     */
    public static boolean isJobSuitableForCareerLevel(
            SaraminJobListResponse.Jobs.Job job,
            double userCareerYears) {

        if (job.position() == null || job.position().experienceLevel() == null
                || job.position().experienceLevel().name() == null) {
            // 경력 정보가 없으면 보수적으로 적합하다고 판단
            return true;
        }

        String experienceLevel = job.position().experienceLevel().name();
        log.debug("[JobMatchingUtil] Checking job {} - experience level: '{}'",
                job.id(), experienceLevel);

        // 1. 신입 또는 경력무관
        if (experienceLevel.contains("신입") || experienceLevel.contains("경력무관")) {
            return true;
        }

        // 2. "경력 X년↑" 패턴 (예: "경력 3년↑", "경력 5년 이상")
        Pattern minYearsPattern = Pattern.compile("경력\\s*(\\d+)\\s*년\\s*[↑이상]");
        Matcher minMatcher = minYearsPattern.matcher(experienceLevel);
        if (minMatcher.find()) {
            int requiredMinYears = Integer.parseInt(minMatcher.group(1));
            boolean suitable = userCareerYears >= requiredMinYears;
            log.debug("[JobMatchingUtil] Min years required: {}, user has: {}, suitable: {}",
                    requiredMinYears, String.format("%.2f", userCareerYears), suitable);
            return suitable;
        }

        // 3. "경력 X~Y년" 패턴 (예: "경력 3~7년")
        Pattern rangePattern = Pattern.compile("경력\\s*(\\d+)\\s*~\\s*(\\d+)\\s*년");
        Matcher rangeMatcher = rangePattern.matcher(experienceLevel);
        if (rangeMatcher.find()) {
            int minYears = Integer.parseInt(rangeMatcher.group(1));
            int maxYears = Integer.parseInt(rangeMatcher.group(2));
            boolean suitable = userCareerYears >= minYears && userCareerYears <= maxYears;
            log.debug("[JobMatchingUtil] Required range: {}-{} years, user has: {}, suitable: {}",
                    minYears, maxYears, String.format("%.2f", userCareerYears), suitable);
            return suitable;
        }

        // 4. 파싱할 수 없는 경우 보수적으로 적합하다고 판단
        log.debug("[JobMatchingUtil] Could not parse experience level, allowing job");
        return true;
    }
}

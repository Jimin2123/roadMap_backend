package com.shingu.roadmap.apis.openai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 훈련 과정 우선순위 계산 유틸리티 (Phase 3 A3)
 *
 * 다차원 평가를 통해 훈련 과정의 우선순위를 계산합니다:
 * 1. Gap 크기 (현재 역량 vs 목표 역량 차이)
 * 2. 역량 중요도 (핵심 역량 여부)
 * 3. 학습 난이도 (초급 과정 우선)
 * 4. 수강 가능성 (지역, 시간, 비용)
 * 5. 완료 가능성 (과정 기간, 난이도)
 */
@Slf4j
@Component
public class TrainingCoursePrioritizer {

    /**
     * 훈련 과정 우선순위 정보
     *
     * @param courseName 과정 이름
     * @param priorityScore 우선순위 점수 (0-100)
     * @param gap 역량 갭 크기 (0.0-1.0)
     * @param importance 역량 중요도 (0.0-1.0)
     * @param difficulty 학습 난이도 (BEGINNER, INTERMEDIATE, ADVANCED)
     * @param recommendation 추천 이유
     */
    public record CoursePriority(
            String courseName,
            double priorityScore,
            double gap,
            double importance,
            String difficulty,
            String recommendation
    ) implements Comparable<CoursePriority> {
        @Override
        public int compareTo(CoursePriority other) {
            // 우선순위 점수가 높을수록 먼저 정렬
            return Double.compare(other.priorityScore, this.priorityScore);
        }
    }

    /**
     * 훈련 과정 우선순위 점수를 계산합니다.
     *
     * 우선순위 점수 = (Gap 크기 * 40%) + (역량 중요도 * 30%) +
     *                (학습 용이성 * 20%) + (수강 가능성 * 10%)
     *
     * @param gap 역량 갭 (0.0 ~ 1.0, 높을수록 차이 큼)
     * @param importance 역량 중요도 (0.0 ~ 1.0, 높을수록 중요)
     * @param difficulty 학습 난이도 (BEGINNER, INTERMEDIATE, ADVANCED)
     * @param accessibility 수강 가능성 (0.0 ~ 1.0, 높을수록 수강 용이)
     * @return 우선순위 점수 (0-100)
     */
    public double calculatePriorityScore(
            double gap,
            double importance,
            String difficulty,
            double accessibility) {

        // 1. Gap 크기 점수 (40%)
        double gapScore = gap * 40.0;

        // 2. 역량 중요도 점수 (30%)
        double importanceScore = importance * 30.0;

        // 3. 학습 용이성 점수 (20%) - 난이도에 반비례
        double learningEaseScore = calculateLearningEaseScore(difficulty) * 20.0;

        // 4. 수강 가능성 점수 (10%)
        double accessibilityScore = accessibility * 10.0;

        double totalScore = gapScore + importanceScore + learningEaseScore + accessibilityScore;

        log.debug("[TrainingCoursePrioritizer] Score calculation - gap: {}, importance: {}, ease: {}, access: {} → total: {}",
                gapScore, importanceScore, learningEaseScore, accessibilityScore, totalScore);

        return Math.min(100.0, totalScore); // Cap at 100
    }

    /**
     * 난이도에 따른 학습 용이성 점수 계산
     * 초급 과정일수록 높은 점수
     */
    private double calculateLearningEaseScore(String difficulty) {
        if (difficulty == null) {
            return 0.5; // Default
        }

        return switch (difficulty.toUpperCase()) {
            case "BEGINNER", "초급", "입문" -> 1.0;
            case "INTERMEDIATE", "중급" -> 0.6;
            case "ADVANCED", "고급", "심화" -> 0.3;
            default -> 0.5;
        };
    }

    /**
     * 훈련 과정 리스트에 우선순위를 부여하고 정렬합니다.
     *
     * @param courses 훈련 과정 정보 리스트 (name, gap, importance, difficulty, accessibility)
     * @return 우선순위 순으로 정렬된 과정 리스트
     */
    public List<CoursePriority> prioritizeCourses(List<CourseInfo> courses) {
        log.info("[TrainingCoursePrioritizer] Prioritizing {} courses", courses.size());

        List<CoursePriority> prioritized = courses.stream()
                .map(course -> {
                    double priorityScore = calculatePriorityScore(
                            course.gap(),
                            course.importance(),
                            course.difficulty(),
                            course.accessibility()
                    );

                    String recommendation = generateRecommendation(
                            course.gap(),
                            course.importance(),
                            course.difficulty()
                    );

                    return new CoursePriority(
                            course.name(),
                            priorityScore,
                            course.gap(),
                            course.importance(),
                            course.difficulty(),
                            recommendation
                    );
                })
                .sorted() // CoursePriority.compareTo() 사용
                .collect(Collectors.toList());

        log.info("[TrainingCoursePrioritizer] Prioritization complete - top course: {}, score: {}",
                prioritized.isEmpty() ? "None" : prioritized.get(0).courseName(),
                prioritized.isEmpty() ? 0 : prioritized.get(0).priorityScore());

        return prioritized;
    }

    /**
     * 추천 이유 생성
     */
    private String generateRecommendation(double gap, double importance, String difficulty) {
        StringBuilder reason = new StringBuilder();

        if (gap > 0.7) {
            reason.append("역량 갭이 큼. ");
        } else if (gap > 0.4) {
            reason.append("역량 향상 필요. ");
        }

        if (importance > 0.7) {
            reason.append("핵심 역량. ");
        }

        if ("BEGINNER".equalsIgnoreCase(difficulty) || "초급".equals(difficulty)) {
            reason.append("초급 과정으로 학습 용이.");
        } else if ("INTERMEDIATE".equalsIgnoreCase(difficulty)) {
            reason.append("중급 과정.");
        } else if ("ADVANCED".equalsIgnoreCase(difficulty)) {
            reason.append("고급 과정으로 도전 과제.");
        }

        return reason.toString().trim();
    }

    /**
     * 훈련 과정 입력 정보
     *
     * @param name 과정 이름
     * @param gap 역량 갭 (0.0-1.0)
     * @param importance 중요도 (0.0-1.0)
     * @param difficulty 난이도 (BEGINNER, INTERMEDIATE, ADVANCED)
     * @param accessibility 수강 가능성 (0.0-1.0)
     */
    public record CourseInfo(
            String name,
            double gap,
            double importance,
            String difficulty,
            double accessibility
    ) {}
}

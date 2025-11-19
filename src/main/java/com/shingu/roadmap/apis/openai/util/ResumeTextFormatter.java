package com.shingu.roadmap.apis.openai.util;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.resume.domain.*;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Resume 객체를 텍스트 형식으로 변환하는 유틸리티 클래스
 */
@Component
public class ResumeTextFormatter {

    public String resumeToText(Resume resume) {
        if (resume == null) return "";
        StringBuilder sb = new StringBuilder();

        if (resume.getIntroduction() != null) {
            Introduction intro = resume.getIntroduction();

            // 각 항목(성장과정, 강점, 학교생활, 지원동기)의 내용이 비어있지 않은 경우에만 추가합니다.
            if (intro.getGrowthProcess() != null && !intro.getGrowthProcess().isBlank()) {
                sb.append("성장과정: ").append(intro.getGrowthProcess()).append("\n\n");
            }

            if (intro.getStrengths() != null && !intro.getStrengths().isBlank()) {
                sb.append("장점 및 강점: ").append(intro.getStrengths()).append("\n\n");
            }

            if (intro.getSchoolLife() != null && !intro.getSchoolLife().isBlank()) {
                sb.append("학교생활: ").append(intro.getSchoolLife()).append("\n\n");
            }

            if (intro.getMotivation() != null && !intro.getMotivation().isBlank()) {
                sb.append("지원동기: ").append(intro.getMotivation()).append("\n\n");
            }
        }
        if (resume.getEducation() != null) {
            Education edu = resume.getEducation();
            sb.append("학력: ").append(edu.getSchool()).append(" ").append(edu.getMajor()).append(" ").append(edu.getGpa()).append(" (").append(edu.getStatus()).append(")\n")
                    .append("  기간: ").append(formatPeriod(edu.getPeriod())).append("\n\n");
        }
        if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
            sb.append("프로젝트:\n");
            for (Project p : resume.getProjects()) {
                String techStackString = p.getTechStack().stream().map(Skill::getName).collect(Collectors.joining(", "));
                sb.append("- ").append(p.getName()).append(" (").append(formatPeriod(p.getPeriod())).append(")\n")
                        .append("  역할: ").append(p.getRole()).append("\n")
                        .append("  설명: ").append(p.getDescription()).append("\n")
                        .append("  기술스택: ").append(techStackString).append("\n");
                if (p.getAchievements() != null && !p.getAchievements().isEmpty()) {
                    sb.append("  주요 성과:\n");
                    p.getAchievements().forEach(ach -> sb.append("    - ").append(ach).append("\n"));
                }
                if (p.getUrl() != null && !p.getUrl().isBlank()) {
                    sb.append("  URL: ").append(p.getUrl()).append("\n");
                }
            }
            sb.append("\n");
        }
        if (resume.getActivities() != null && !resume.getActivities().isEmpty()) {
            sb.append("대외활동:\n");
            for (Activity a : resume.getActivities()) {
                sb.append("- ").append(a.getTitle()).append(" (").append(a.getOrganization()).append(")\n")
                        .append("  기간: ").append(formatPeriod(a.getPeriod())).append("\n")
                        .append("  내용: ").append(a.getDescription()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String formatPeriod(Period period) {
        if (period == null || period.getStartDate() == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM");
        String startDate = period.getStartDate().format(formatter);
        if (period.getEndDate() == null) return startDate + " - 진행 중";
        String endDate = period.getEndDate().format(formatter);
        return startDate + " - " + endDate;
    }
}

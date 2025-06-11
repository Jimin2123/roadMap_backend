package com.shingu.roadmap.resume.service;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.repository.SkillRepository;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.service.MemberService;
import com.shingu.roadmap.resume.domain.*;
import com.shingu.roadmap.resume.dto.response.ResumeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

  private final MemberService memberService;
  private final SkillRepository skillRepository;

  public MemberResponse createResume(Long memberId, ProfileRequest request) {
    Resume resume = new Resume(
            null,
            null,
            null,
            null,
            null,
            null
    );

    // 자기소개 설정
    Introduction introduction = new Introduction();
    introduction.setContent(request.resume().introduction().content());
    resume.setIntroduction(introduction);

    // 활동 내역 설정
    List<Activity> activities = request.resume().activities().stream()
        .map(activityRequest -> {
          Activity activity = new Activity();
          activity.setTitle(activityRequest.title());
          activity.setOrganization(activityRequest.organization());
          activity.setPeriod(activityRequest.period());
          activity.setDescription(activityRequest.description());
          activity.setResume(resume);
          return activity;
        }).toList();
    resume.setActivities(activities);

    // 포트폴리오 설정
    List<Portfolio> portfolios = request.resume().portfolios().stream()
        .map(portfolioRequest -> {
          Portfolio portfolio = new Portfolio();
          portfolio.setTitle(portfolioRequest.title());
          portfolio.setUrl(portfolioRequest.url());
          portfolio.setResume(resume);
          return portfolio;
        }).toList();
    resume.setPortfolios(portfolios);

    List<Project> projects = request.resume().projects().stream()
            .map(projectRequest -> {
              Set<Skill> projectSkills = projectRequest.techStack().stream()
                      .map(skillName -> skillRepository.findByName(skillName)
                              .orElseGet(() -> skillRepository.save(new Skill(null, skillName))))
                      .collect(Collectors.toSet());
              Project project = new Project();
              project.setName(projectRequest.title());
              project.setPeriod(projectRequest.period());
              project.setTechStack(projectSkills);
              project.setDescription(projectRequest.description());
              project.setResume(resume);
              return project;
            })
            .toList();


    resume.setProjects(projects);

    // 학력 정보 설정
    Education education = new Education();
    education.setSchool(request.resume().education().school());
    education.setMajor(request.resume().education().major());
    education.setPeriod(request.resume().education().period());
    education.setStatus(request.resume().education().status());
    resume.setEducation(education);

    return memberService.updateProfile(memberId, request, resume);
  }

  public ResumeResponse getResume(Long memberId) {
    ProfileResponse memberProfile = memberService.getProfile(memberId);

    return memberProfile.resume();
  }
}

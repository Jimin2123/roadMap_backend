package com.shingu.roadmap.resume.service;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.repository.SkillRepository;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.service.MemberService;
import com.shingu.roadmap.resume.domain.*;
import com.shingu.roadmap.resume.dto.request.*;
import com.shingu.roadmap.resume.dto.response.ResumeResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

  private final MemberService memberService;
  private final SkillRepository skillRepository;

  @Transactional
  public MemberResponse createResume(Long memberId, ProfileRequest request) {
    Resume resume = new Resume(
            null,
            null,
            null,
            null,
            null
    );
    ResumeRequest resumeRequest = request.resume();

    // 1. Introduction, Education 엔티티 생성 및 설정
    resume.setIntroduction(toIntroductionEntity(resumeRequest.introduction()));
    resume.setEducation(toEducationEntity(resumeRequest.education()));

    // 2. Activity, Project 엔티티 컬렉션 생성 및 설정 (양방향 연관관계 설정 포함)
    List<Activity> activities = resumeRequest.activities().stream()
            .map(activityReq -> toActivityEntity(activityReq, resume))
            .toList();

    List<Project> projects = resumeRequest.projects().stream()
            .map(projectReq -> toProjectEntity(projectReq, resume))
            .toList();

    resume.setActivities(activities);
    resume.setProjects(projects);

    // 3. MemberService를 통해 최종 저장
    return memberService.updateProfile(memberId, request, resume);
  }

  @Transactional(readOnly = true)
  public ResumeResponse getResume(Long memberId) {
    // 1. MemberService를 통해 Member 엔티티를 조회합니다.
    Member member = memberService.findMemberById(memberId);

    // 2. Member 엔티티에서 Resume 엔티티를 가져옵니다.
    Resume resume = member.getProfile().getResume();

    // 3. 이력서가 존재하지 않을 경우 예외를 발생시킵니다.
    if (resume == null) {
      throw new EntityNotFoundException("해당 회원의 이력서 정보를 찾을 수 없습니다.");
    }

    // 4. Resume 엔티티를 ResumeResponse DTO로 변환하여 반환합니다.
    return ResumeResponse.from(resume);
  }

  // ================= Private Helper Methods for Mapping =================

  private Introduction toIntroductionEntity(IntroductionRequest dto) {
    Introduction introduction = new Introduction();
    introduction.setContent(dto.content());
    return introduction;
  }

  private Education toEducationEntity(EducationRequest dto) {
    Education education = new Education();
    education.setSchool(dto.school());
    education.setMajor(dto.major());
    education.setStatus(dto.status());
    education.setPeriod(toPeriod(dto.period())); // Period 변환
    return education;
  }

  private Activity toActivityEntity(ActivityRequest dto, Resume resume) {
    Activity activity = new Activity();
    activity.setTitle(dto.title());
    activity.setOrganization(dto.organization());
    activity.setDescription(dto.description());
    activity.setPeriod(toPeriod(dto.period())); // Period 변환
    activity.setResume(resume); // 양방향 연관관계 설정
    return activity;
  }

  private Project toProjectEntity(ProjectRequest dto, Resume resume) {
    Project project = new Project();
    project.setName(dto.name());
    project.setDescription(dto.description());
    project.setUrl(dto.url());
    project.setRole(dto.role());
    project.setAchievements(dto.achievements());
    project.setPeriod(toPeriod(dto.period())); // Period 변환

    // "Find or Create" 로직을 사용한 techStack 처리
    Set<Skill> techStack = dto.techStack().stream()
            .map(this::findOrCreateSkill)
            .collect(Collectors.toSet());
    project.setTechStack(techStack);

    project.setResume(resume); // 양방향 연관관계 설정
    return project;
  }

  private Skill findOrCreateSkill(String skillName) {
    return skillRepository.findByName(skillName)
            .orElseGet(() -> skillRepository.save(new Skill(null, skillName)));
  }

  private Period toPeriod(PeriodRequest dto) {
    if (dto == null) {
      return null;
    }
    return new Period(dto.startDate(), dto.endDate());
  }
}
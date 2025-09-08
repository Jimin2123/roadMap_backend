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
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

  private final MemberService memberService;
  private final SkillRepository skillRepository;

  /* ============================ Commands ============================ */

  @Transactional
  public MemberResponse createResume(Long memberId, ProfileRequest request) {
    if (request == null || request.resume() == null) {
      throw new IllegalArgumentException("ResumeRequest가 없습니다.");
    }
    ResumeRequest resumeReq = request.resume();

    // 1) 빈 Resume 생성 (builder)
    Resume resume = Resume.builder().build();

    // 2) Introduction / Education 세팅 (단방향 1:1)
    if (resumeReq.introduction() != null) {
      resume.setIntroduction(toIntroduction(resumeReq.introduction()));
    }
    if (resumeReq.education() != null) {
      resume.setEducation(toEducation(resumeReq.education()));
    }

    // 3) Activities / Projects 조립 (양방향은 Resume 편의 메서드로만 연결)
    if (!CollectionUtils.isEmpty(resumeReq.activities())) {
      for (ActivityRequest aReq : resumeReq.activities()) {
        Activity a = toActivity(aReq);
        resume.addActivity(a); // 내부에서 setResumeInternal 처리
      }
    }

    if (!CollectionUtils.isEmpty(resumeReq.projects())) {
      for (ProjectRequest pReq : resumeReq.projects()) {
        Project p = toProjectSkeleton(pReq);     // 기간/기본정보만
        attachProjectExtras(p, pReq);            // achievements, techStack
        resume.addProject(p);                    // 내부에서 setResumeInternal 처리
      }
    }

    // 4) MemberService로 위임하여 Profile에 Resume 장착 및 전체 저장
    return memberService.updateProfile(memberId, request, resume);
  }

  /* ============================ Queries ============================ */

  @Transactional(readOnly = true)
  public ResumeResponse getResume(Long memberId) {
    Member member = memberService.findMemberById(memberId);
    if (member.getProfile() == null) {
      throw new EntityNotFoundException("해당 회원의 프로필이 존재하지 않습니다.");
    }
    Resume resume = member.getProfile().getResume();
    if (resume == null) {
      throw new EntityNotFoundException("해당 회원의 이력서 정보를 찾을 수 없습니다.");
    }
    return ResumeResponse.from(resume);
  }

  /* ============================ Mappers ============================ */

  private Introduction toIntroduction(IntroductionRequest dto) {
    // 도메인은 updateContent도 있지만 최초 생성은 builder 사용
    return Introduction.builder()
            .content(dto != null ? dto.content() : null)
            .build();
  }

  private Education toEducation(EducationRequest dto) {
    if (dto == null) return null;
    return Education.builder()
            .school(dto.school())
            .major(dto.major())
            .status(dto.status())
            .period(toPeriod(dto.period()))
            .build();
  }

  private Activity toActivity(ActivityRequest dto) {
    if (dto == null) return null;
    return Activity.builder()
            .title(dto.title())
            .organization(dto.organization())
            .description(dto.description())
            .period(toPeriod(dto.period()))
            .build();
  }

  /**
   * Project 골격만 생성 (성과/스택은 별도 attach)
   */
  private Project toProjectSkeleton(ProjectRequest dto) {
    if (dto == null) return null;
    return Project.builder()
            .name(dto.name())
            .url(dto.url())
            .role(dto.role())
            .description(dto.description())
            .period(toPeriod(dto.period()))
            .build();
  }

  /**
   * Project에 achievements/techStack 연결
   * - achievements: addAchievement로 추가
   * - techStack: find-or-create 후 Set에 addAll
   */
  private void attachProjectExtras(Project project, ProjectRequest dto) {
    if (project == null || dto == null) return;

    // achievements
    if (!CollectionUtils.isEmpty(dto.achievements())) {
      // 내부 컬렉션은 getAchievements()로 접근
      project.getAchievements().clear();
      for (String a : dto.achievements()) {
        project.addAchievement(a);
      }
    }

    // techStack
    if (!CollectionUtils.isEmpty(dto.techStack())) {
      Set<Skill> stack = dto.techStack().stream()
              .map(this::findOrCreateSkill)
              .collect(Collectors.toSet());
      project.getTechStack().clear();
      project.getTechStack().addAll(stack);
    }
  }

  private Skill findOrCreateSkill(String skillName) {
    return skillRepository.findByName(skillName)
            .orElseGet(() -> skillRepository.save(Skill.builder().name(skillName).build()));
  }

  private Period toPeriod(PeriodRequest dto) {
    if (dto == null) return null;
    return Period.of(dto.startDate(), dto.endDate()); // 유효성 내장
  }
}
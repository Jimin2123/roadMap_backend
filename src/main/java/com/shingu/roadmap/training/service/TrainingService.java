package com.shingu.roadmap.training.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.dto.request.GptTrainingCourseDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.qnet.dto.response.QnetExamScheduleResponse;
import com.shingu.roadmap.apis.qnet.service.QnetService;
import com.shingu.roadmap.apis.work24.dto.response.EmpPgmListResponse;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.apis.work24.service.Work24Service;
import com.shingu.roadmap.member.domain.Address;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.training.repository.EmploymentCenterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {
  private final MemberRepository memberRepository;
  private final EmploymentCenterRepository employmentCenterRepository;
  private final Work24Service work24Service;
  private final OpenAiService openAiService;
  private final QnetService qnetService;

  /**
   * 회원의 프로필, 보유 기술, 자격증, 희망 직무 정보를 기반으로 맞춤형 직업훈련 과정을 추천합니다.
   * 추천 과정은 사용 가능한 훈련 과정들에 대해 AI 기반 분석을 통해 생성됩니다.
   *
   * @param memberId 훈련 과정을 추천받을 대상 회원의 고유 식별자
   * @return {@code TrainingCourseResponse.TrainCourseItem} 형태의 추천 훈련 과정 리스트.
   *         추천이 생성되지 않은 경우 {@code null}을 반환할 수 있습니다.
   * @throws EntityNotFoundException 지정된 ID를 가진 회원이 존재하지 않거나 삭제된 경우 발생합니다.
   */
  @Transactional(readOnly = true)
  public List<TrainingCourseResponse.TrainCourseItem> recommendCoursesForMember(Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Member not found"));

    Profile profile = member.getProfile();

    if (profile == null) {
      throw new EntityNotFoundException("Profile not found for member ID: " + memberId);
    }

    List<String> ncsCodes = profile.getUserCapabilities().stream().map(NcsOccupation::getDutyCd).collect(Collectors.toList());
    String address = member.getAddress().getAddress();
    List<TrainingCourseResponse.TrainCourseItem> trainingList = work24Service.getAllMatchingCourses(ncsCodes, address);

    List<GptTrainingCourseDto> trainings = trainingList.stream()
            .map(item -> new GptTrainingCourseDto(
                    item.trprId(),
                    item.ncsCd(),
                    item.title(),
                    item.address()
            ))
            .toList();

    ProfileResponse profileResponse = ProfileResponse.from(profile);
    TrainingRecommendationRequest request = new TrainingRecommendationRequest(profileResponse, trainings, address);

    Set<String> aiResponse = openAiService.recommendTrainingCourse(request).block();
    if (CollectionUtils.isEmpty(aiResponse)) {
      throw new RuntimeException("No training courses found for member ID: " + memberId);
    }

    return trainingList.stream()
            .filter(item -> aiResponse.contains(item.trprId()))
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<EmpPgmListResponse.EmpPgmSchdInvite> getTrainingProgramsForMember(Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Member not found"));

    Address address = member.getAddress();
    if (address == null) {
      throw new EntityNotFoundException("Address not found for member ID: " + memberId);
    }

    return work24Service.getTrainingPrograms(address);
  }

  public List<QnetExamScheduleResponse.Item> getQnetExamSchedule(String qualgbcd, String jmcd) {
    return qnetService.getExamSchedule(qualgbcd,jmcd);
  }
}

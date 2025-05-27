package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.dto.request.GptTrainingCourseDto;
import com.shingu.roadmap.apis.openai.dto.request.GptUserProfileDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.apis.work24.service.Work24Service;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.NullServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final OpenAiService openAiService;
    private final NcsApiService ncsApiService;
    private final Work24Service work24Service;

//    @Transactional
//    public MemberResponse updateProfile(Long memberId, ProfileRequest request) {
//        Member member = memberRepository.findById(memberId)
//                .filter(m -> m.getDeletedAt() == null)
//                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
//
//        member.applyProfile(request);
//
//        if(!CollectionUtils.isEmpty(request.skills()) ||
//                !CollectionUtils.isEmpty(request.certificates())) {
//            Set<String> recommendedNcsCodes = openAiService.recommendNcsCodeUsingAssistant(member).block();
//
//            if(!CollectionUtils.isEmpty(recommendedNcsCodes)) {
//                Set<NcsOccupation> validCodes = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);
//                member.updateNcsOccupations(validCodes);
//            }
//        }
//        return MemberResponse.from(member);
//    }

    /**
     * 회원의 프로필, 보유 기술, 자격증, 희망 직무 정보를 기반으로 맞춤형 직업훈련 과정을 추천합니다.
     * 추천 과정은 사용 가능한 훈련 과정들에 대해 AI 기반 분석을 통해 생성됩니다.
     *
     * @param memberId 훈련 과정을 추천받을 대상 회원의 고유 식별자
     * @return {@code TrainingCourseResponse.TrainCourseItem} 형태의 추천 훈련 과정 리스트.
     *         추천이 생성되지 않은 경우 {@code null}을 반환할 수 있습니다.
     * @throws EntityNotFoundException 지정된 ID를 가진 회원이 존재하지 않거나 삭제된 경우 발생합니다.
     */
//    public List<TrainingCourseResponse.TrainCourseItem> recommendCoursesForMember(Long memberId) {
//        Member member = memberRepository.findById(memberId)
//                .filter(m -> m.getDeletedAt() == null)
//                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
//
//         // 사용자 정보 정규화
//        GptUserProfileDto userProfile = new GptUserProfileDto(
//                member.getSkills(),
//                member.getCertificates(),
//                member.getProfile().getDesiredJob(),
//                member.getProfile().getEducationLevel().name(),
//                member.getProfile().getMajor(),
//                member.getNcsOccupations().stream().collect(Collectors.toMap(NcsOccupation::getDutyCd, NcsOccupation::getDutyNm)
//                )
//        );
//
//        List<String> ncsCodes = member.getNcsOccupations().stream().map(NcsOccupation::getDutyCd).toList();
//        List<TrainingCourseResponse.TrainCourseItem> trainingList = work24Service.getAllMatchingCourses(ncsCodes);
//
//        List<GptTrainingCourseDto> trainings = trainingList.stream()
//                .map(item -> new GptTrainingCourseDto(
//                        item.trprId(),
//                        item.ncsCd(),
//                        item.title(),
//                        item.address()
//                ))
//                .toList();
//
//        TrainingRecommendationRequest request = new TrainingRecommendationRequest(userProfile, trainings);
//
//        Set<String> aiResponse = openAiService.recommendTrainingCourse(request).block();
//        if (CollectionUtils.isEmpty(aiResponse)) {
//            return null;
//        }
//        return trainingList.stream()
//                .filter(item -> aiResponse.contains(item.trprId()))
//                .collect(Collectors.toList());
//    }
}

package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.dto.request.GptTrainingCourseDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.apis.work24.service.Work24Service;
import com.shingu.roadmap.auth.domain.Account;
import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.member.domain.*;
import com.shingu.roadmap.member.dto.request.AddressRequest;
import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.repository.CertificateRepository;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.member.repository.SkillRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;
    private final CertificateRepository certificateRepository;
    private final OpenAiService openAiService;
    private final NcsApiService ncsApiService;
    private final Work24Service work24Service;

    @Transactional
    public MemberResponse signUp(MemberRequest request) {

        LoginRequest accReq = request.loginRequest();
        Account account = new Account(
                null,
                accReq.email(),
                accReq.password(),
                null,
                null
        );

        AddressRequest addrReq = request.addressRequest();
        Address address = new Address(
                null,
                addrReq.address(),
                addrReq.addressJibun(),
                addrReq.addressDetail(),
                addrReq.regionCity(),
                addrReq.zoncode(),
                null
        );

        Member member = new Member(
                null,
                request.name(),
                "USER", // 기본 역할은 USER로 설정
                request.birthDate(),
                request.phoneNumber(),
                account,
                address,
                null, // 프로필은 나중에 업데이트
                null,
                null
        );

        member.setAccount(account);
        member.setAddress(address);

        memberRepository.save(member);

        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        Profile profile = new Profile(
                null,
                request.educationLevel().name(),
                request.major(),
                request.desiredJob(),
                new HashSet<>(), // certificates
                new HashSet<>(), // skills
                new HashSet<>(), // desiredCapabilities
                new HashSet<>()  // userCapabilities
        );

        // 사용자 보유 기술 등록
        if(!CollectionUtils.isEmpty(request.skills())) {
            Set<Skill> skills = request.skills().stream()
                    .map(skillName -> skillRepository.findByName(skillName)
                            .orElseGet(() -> skillRepository.save(new Skill(null, skillName))))
                    .collect(Collectors.toSet());

            profile.getSkills().addAll(skills);
        }

        // 사용자 보유 국가 자격증 등록
        if (!CollectionUtils.isEmpty(request.certificates())) {
            Set<Certificate> certificates = request.certificates().stream()
                    .map(certificateRepository::findByJmfldnm)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());

            profile.getCertificates().clear();
            profile.getCertificates().addAll(certificates);
        } else {
            profile.getCertificates().clear();
        }

        // 사용자 정보 기반 NCS 코드 추천
        if(!CollectionUtils.isEmpty(profile.getSkills()) &&
                !CollectionUtils.isEmpty(profile.getCertificates())) {

            // 기술 및 자격증 이름 추출
            Set<String> skillNames = profile.getSkills().stream()
                    .map(Skill::getName)
                    .collect(Collectors.toSet());
            Set<String> certificateNames = profile.getCertificates().stream()
                    .map(Certificate::getJmfldnm)
                    .collect(Collectors.toSet());

            Set<String> recommendedNcsCodes = openAiService.recommendNcsCodeUsingAssistant(skillNames, certificateNames).block();

            if(!CollectionUtils.isEmpty(recommendedNcsCodes)) {
                Set<NcsOccupation> validCodes = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);

                profile.getUserCapabilities().clear();
                profile.getUserCapabilities().addAll(validCodes);
            }
        }

        // 희망 직무 기반 NCS 코드 추천
        if(request.desiredJob() != null) {
            Set<String> recommendedNcsCodes = openAiService.recommendDesiredJobCodeUsingAssistant(request.desiredJob()).block();

            if(!CollectionUtils.isEmpty(recommendedNcsCodes)) {
                Set<NcsOccupation> validCodes = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);

                profile.getDesiredCapabilities().clear();
                profile.getDesiredCapabilities().addAll(validCodes);
            }
        }

        member.setProfile(profile);

        return MemberResponse.from(member);
    }

    public List<String> getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        Profile profile = member.getProfile();
        if (profile == null) {
            throw new EntityNotFoundException("Profile not found for member ID: " + member.getId());
        }

        List<String> ncsCodes = profile.getUserCapabilities().stream().map(NcsOccupation::getDutyCd).toList();
        System.out.println(ncsCodes);

        return ncsCodes;
    }

    /**
     * 회원의 프로필, 보유 기술, 자격증, 희망 직무 정보를 기반으로 맞춤형 직업훈련 과정을 추천합니다.
     * 추천 과정은 사용 가능한 훈련 과정들에 대해 AI 기반 분석을 통해 생성됩니다.
     *
     * @param memberId 훈련 과정을 추천받을 대상 회원의 고유 식별자
     * @return {@code TrainingCourseResponse.TrainCourseItem} 형태의 추천 훈련 과정 리스트.
     *         추천이 생성되지 않은 경우 {@code null}을 반환할 수 있습니다.
     * @throws EntityNotFoundException 지정된 ID를 가진 회원이 존재하지 않거나 삭제된 경우 발생합니다.
     */
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

}

package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.apis.saramin.repository.SaraminJobRepository;
import com.shingu.roadmap.auth.domain.Account;
import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.common.domain.Certificate;
import com.shingu.roadmap.member.domain.*;
import com.shingu.roadmap.member.dto.request.AddressRequest;
import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.common.repository.CertificateRepository;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.member.repository.SkillRepository;
import com.shingu.roadmap.resume.domain.Resume;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
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
    private final SaraminJobRepository saraminJobRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signUp(MemberRequest request) {

        LoginRequest accReq = request.loginRequest();
        String encodedPassword = passwordEncoder.encode(accReq.password());
        Account account = new Account(
                null,
                accReq.email(),
                encodedPassword,
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
                addrReq.zonecode(),
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
    public MemberResponse updateProfile(Long memberId, ProfileRequest request, Resume resume) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        Profile profile = new Profile(
                null,                             // id (auto)
                request.educationLevel().name(),      // 학력
                new HashSet<>(),                      // ★ 희망 직무(FK)
                new HashSet<>(),                      // certificates
                new HashSet<>(),                      // skills
                new HashSet<>(),                      // desiredCapabilities
                new HashSet<>(),                      // userCapabilities
                resume
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
            profile.getProfileCertificates().clear();

            Set<ProfileCertificate> newCertificates = request.certificates().stream()
                    .map(certReq -> {
                        Certificate certificate = certificateRepository.findByJmfldnm(certReq.name())
                                .orElseThrow(() -> new RuntimeException("자격증을 찾을 수 없습니다: " + certReq.name()));
                        return new ProfileCertificate(profile, certificate, certReq.year());
                    })
                    .collect(Collectors.toSet());

            profile.getProfileCertificates().addAll(newCertificates);
        } else {
            profile.getProfileCertificates().clear();
        }

        // 사용자 정보 기반 NCS 코드 추천
        if(!CollectionUtils.isEmpty(profile.getSkills()) &&
                !CollectionUtils.isEmpty(profile.getProfileCertificates())) {

            // 기술 및 자격증 이름 추출
            Set<String> skillNames = profile.getSkills().stream()
                    .map(Skill::getName)
                    .collect(Collectors.toSet());
            Set<String> certificateNames = profile.getProfileCertificates().stream()
                    .map(pc -> pc.getCertificate().getJmfldnm())
                    .collect(Collectors.toSet());

            Set<String> recommendedNcsCodes = openAiService.recommendNcsCodeUsingAssistant(skillNames, certificateNames, resume).block();

            if(!CollectionUtils.isEmpty(recommendedNcsCodes)) {
                Set<NcsOccupation> validCodes = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);

                profile.getUserCapabilities().clear();
                profile.getUserCapabilities().addAll(validCodes);
            }
        }

        // 희망 직무 기반 NCS 코드 추천
        if (!profile.getDesiredJobs().isEmpty()) {

            Set<String> ncsCandidates = new HashSet<>();

            for (SaraminJob job : profile.getDesiredJobs()) {
                Set<String> rec = openAiService
                        .recommendDesiredJobCodeUsingAssistant(job.getName())
                        .block();

                if (!CollectionUtils.isEmpty(rec)) {
                    ncsCandidates.addAll(rec);// 누적
                }
            }

            if (!ncsCandidates.isEmpty()) {
                Set<NcsOccupation> valid = ncsApiService.filterValidNcsCodes(ncsCandidates);

                profile.getDesiredCapabilities().clear();
                profile.getDesiredCapabilities().addAll(valid);
            }
        }

        if (!CollectionUtils.isEmpty(request.desiredJobCodes())) {
            Set<SaraminJob> jobs = request.desiredJobCodes().stream()
                    .map(code -> saraminJobRepository.findById(code)
                            .orElseThrow(() -> new IllegalArgumentException("직무 코드 없음: " + code)))
                    .collect(Collectors.toSet());
            profile.getDesiredJobs().addAll(jobs);

            // 희망 직무 이름을 하나의 문자열로 결합
            String combinedJobNames = jobs.stream().map(SaraminJob::getName).collect(Collectors.joining(", "));

            // OpenAI를 사용하여 희망 직무 이름으로 NCS 코드 추천
            Set<String> recommendedNcsCodes = openAiService.recommendDesiredJobCodeUsingAssistant(combinedJobNames).block();

            if(!CollectionUtils.isEmpty(recommendedNcsCodes)) {
                Set<NcsOccupation> validCodes = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);

                profile.getDesiredCapabilities().clear();
                profile.getDesiredCapabilities().addAll(validCodes);
            }
        }

        member.setProfile(profile);

        return MemberResponse.from(member);
    }

    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        return MemberResponse.from(member);
    }

    public ProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        Profile profile = member.getProfile();
        if (profile == null) {
            throw new EntityNotFoundException("Profile not found for member ID: " + member.getId());
        }

      return ProfileResponse.from(profile);
    }
}

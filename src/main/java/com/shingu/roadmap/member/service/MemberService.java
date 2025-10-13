package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.apis.saramin.repository.SaraminJobRepository;
import com.shingu.roadmap.auth.domain.Account;
import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.common.domain.Certificate;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.enums.MemberRole;
import com.shingu.roadmap.common.repository.CertificateRepository;
import com.shingu.roadmap.common.repository.SkillRepository;
import com.shingu.roadmap.member.domain.Email;
import com.shingu.roadmap.member.domain.*;
import com.shingu.roadmap.member.dto.request.AddressRequest;
import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.resume.domain.*;
import com.shingu.roadmap.member.exception.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;
    private final CertificateRepository certificateRepository;
    private final OpenAiService openAiService;
    private final NcsApiService ncsApiService;
    private final SaraminJobRepository saraminJobRepository;
    private final PasswordEncoder passwordEncoder;

    /* ====================================================================== */
    /* Public Usecases                                                        */
    /* ====================================================================== */

    public MemberResponse signUp(MemberRequest request) {
        // 이메일 중복 체크
        if (memberRepository.findByAccountEmail(request.loginRequest().email()).isPresent()) {
            throw new DuplicateMemberException(request.loginRequest().email());
        }

        Member member = assembleMember(request);
        memberRepository.save(member);
        return MemberResponse.from(member);
    }

    /**
     * 프로필 + 이력서(선택) 업데이트
     * - Resume 하위 엔티티는 도메인 편의 메서드로만 연결(add/set)하도록 권장
    */
    public MemberResponse updateProfile(Long memberId, ProfileRequest req, Resume resume) {
        Member member = findMember(memberId);

        // 기존 프로필 조립
        Profile profile = assembleProfile(req, resume);

        // 스킬/희망직무 보강
        enrichWithSkills(req, profile);
        enrichWithDesiredJobs(req, profile);

        member.setProfile(profile);
        return MemberResponse.from(member);
    }

    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findMember(memberId));
    }

    public ProfileResponse getProfile(Long memberId) {
        Profile profile = findMember(memberId).getProfile();
        if (profile == null) throw new ProfileNotFoundException(memberId);
        return ProfileResponse.from(profile);
    }

    /**
     * 프로필 정보만 업데이트 (이력서 제외)
     * - 기존 프로필이 있으면 업데이트하고, 없으면 새로 생성합니다.
     * - 기존 이력서는 유지됩니다.
     * - 전화번호와 주소도 함께 업데이트합니다.
     */
    public ProfileResponse updateProfileOnly(Long memberId, com.shingu.roadmap.member.dto.request.ProfileUpdateRequest req) {
        Member member = findMember(memberId);

        // 전화번호 업데이트
        if (req.phoneNumber() != null) {
            member.changePhone(req.phoneNumber());
        }

        // 주소 업데이트
        if (req.address() != null) {
            Address newAddress = createAddress(req.address());
            member.setAddress(newAddress);
        }

        // 기존 프로필 가져오기 또는 새로 생성
        Profile profile = member.getProfile();
        Resume existingResume = null;

        if (profile != null) {
            // 기존 프로필이 있으면 기존 이력서 보존
            existingResume = profile.getResume();
        }

        // 새 프로필 조립 (기존 이력서 유지)
        Profile newProfile = Profile.builder()
                .educationLevel(req.educationLevel() != null ? req.educationLevel().name() : null)
                .profileImageUrl(req.profileImageUrl())
                .desiredJobs(new HashSet<>())
                .profileSkills(new HashSet<>())
                .desiredCapabilities(new HashSet<>())
                .userCapabilities(new HashSet<>())
                .resume(existingResume)
                .build();

        // 스킬/희망직무 보강
        enrichWithSkillsForUpdate(req, newProfile);
        enrichWithDesiredJobsForUpdate(req, newProfile);

        member.setProfile(newProfile);
        return ProfileResponse.from(newProfile);
    }

    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    /* ====================================================================== */
    /* Assemble Helpers                                                       */
    /* ====================================================================== */

    private Member assembleMember(MemberRequest request) {
        Account account = createAccount(request.loginRequest());
        Address address = createAddress(request.addressRequest());

        return Member.builder()
                .name(request.name())
                .role(MemberRole.USER.getValue())
                .birthDate(request.birthDate())
                .phoneNumber(request.phoneNumber())
                .account(account)
                .address(address)
                .build();
    }

    private Account createAccount(LoginRequest req) {
        return Account.builder()
                .email(Email.of(req.email()))
                .password(passwordEncoder.encode(req.password()))
                .build();
    }

    private Address createAddress(AddressRequest req) {
        if (req == null) return null;
        return Address.builder()
                .address(req.address())
                .addressJibun(req.addressJibun())
                .addressDetail(req.addressDetail())
                .regionCity(req.regionCity())
                .zonecode(req.zonecode())
                .build();
    }

    private Profile assembleProfile(ProfileRequest req, Resume resume) {
        return Profile.builder()
                .educationLevel(req.educationLevel() != null ? req.educationLevel().name() : null)
                .profileImageUrl(req.profileImageUrl())
                .desiredJobs(new HashSet<>())
                .profileSkills(new HashSet<>())
                .desiredCapabilities(new HashSet<>())
                .userCapabilities(new HashSet<>())
                .resume(resume) // ← Resume를 통째로 세팅(도메인 내부 add*/set* 사용해 조립해 들어온 객체)
                .build();
    }

    /* ====================================================================== */
    /* Profile Enrichers                                                      */
    /* ====================================================================== */

    private void enrichWithSkills(ProfileRequest req, Profile profile) {
        profile.getProfileSkills().clear();
        if (req == null || CollectionUtils.isEmpty(req.skills())) return;

        for (var skillReq : req.skills()) {
            Skill skill = skillRepository.findByName(skillReq.name())
                    .orElseGet(() -> skillRepository.save(Skill.builder().name(skillReq.name()).build()));

            ProfileSkill ps = ProfileSkill.of(profile, skill, skillReq.proficiency());
            profile.addSkill(ps);
        }
    }

    private void enrichWithDesiredJobs(ProfileRequest req, Profile profile) {
        if (req == null || CollectionUtils.isEmpty(req.desiredJobCodes())) return;

        Set<SaraminJob> jobs = req.desiredJobCodes().stream()
                .map(code -> saraminJobRepository.findById(code)
                        .orElseThrow(() -> new JobCodeNotFoundException(String.valueOf(code))))
                .collect(Collectors.toSet());
        profile.getDesiredJobs().addAll(jobs);
    }

    private void enrichWithSkillsForUpdate(com.shingu.roadmap.member.dto.request.ProfileUpdateRequest req, Profile profile) {
        profile.getProfileSkills().clear();
        if (req == null || CollectionUtils.isEmpty(req.skills())) return;

        for (var skillReq : req.skills()) {
            Skill skill = skillRepository.findByName(skillReq.name())
                    .orElseGet(() -> skillRepository.save(Skill.builder().name(skillReq.name()).build()));

            ProfileSkill ps = ProfileSkill.of(profile, skill, skillReq.proficiency());
            profile.addSkill(ps);
        }
    }

    private void enrichWithDesiredJobsForUpdate(com.shingu.roadmap.member.dto.request.ProfileUpdateRequest req, Profile profile) {
        if (req == null || CollectionUtils.isEmpty(req.desiredJobCodes())) return;

        Set<SaraminJob> jobs = req.desiredJobCodes().stream()
                .map(code -> saraminJobRepository.findById(code)
                        .orElseThrow(() -> new JobCodeNotFoundException(String.valueOf(code))))
                .collect(Collectors.toSet());
        profile.getDesiredJobs().addAll(jobs);
    }

    /* ====================================================================== */
    /* NCS 추천                                                               */
    /* ====================================================================== */

    private void recommendCapabilities(Profile profile) {
        recommendUserCapabilities(profile);
        recommendDesiredCapabilities(profile);
    }

    private void recommendUserCapabilities(Profile profile) {
        Resume resume = profile.getResume();
        if (profile.getProfileSkills().isEmpty() && (resume == null || resume.getCertificates().isEmpty())) return;

        Set<String> rec = openAiService.recommendNcsCodeUsingAssistant(profile)
                .blockOptional().orElseGet(HashSet::new);

        if (!rec.isEmpty()) {
            Set<NcsOccupation> valid = ncsApiService.filterValidNcsCodes(rec);
            profile.getUserCapabilities().clear();
            profile.getUserCapabilities().addAll(valid);
        }
    }

    private void recommendDesiredCapabilities(Profile profile) {
        if (profile.getDesiredJobs().isEmpty()) return;

        Set<String> names = profile.getDesiredJobs().stream()
                .map(SaraminJob::getName)
                .collect(Collectors.toSet());

        Set<String> rec = openAiService.recommendDesiredJobCodeUsingAssistant(String.join(", ", names))
                .blockOptional().orElseGet(HashSet::new);

        if (!rec.isEmpty()) {
            Set<NcsOccupation> valid = ncsApiService.filterValidNcsCodes(rec);
            profile.getDesiredCapabilities().clear();
            profile.getDesiredCapabilities().addAll(valid);
        }
    }

    /* ====================================================================== */
    /* Utils                                                                  */
    /* ====================================================================== */

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }
}
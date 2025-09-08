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
import com.shingu.roadmap.common.repository.CertificateRepository;
import com.shingu.roadmap.common.repository.SkillRepository;
import com.shingu.roadmap.member.domain.*;
import com.shingu.roadmap.member.dto.request.AddressRequest;
import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.resume.domain.Resume;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    // 의존성 주입
    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;
    private final CertificateRepository certificateRepository;
    private final OpenAiService openAiService;
    private final NcsApiService ncsApiService;
    private final SaraminJobRepository saraminJobRepository;
    private final PasswordEncoder passwordEncoder;

    // ────────────────────────────────────────────────────────────────────────────────
    // 퍼블릭 유스케이스
    // ────────────────────────────────────────────────────────────────────────────────

    /** 회원 가입 */
    public MemberResponse signUp(MemberRequest request) {
        Member member = assembleMember(request);
        memberRepository.save(member);
        return MemberResponse.from(member);
    }

    /** 프로필 업데이트 */
    public MemberResponse updateProfile(Long memberId, ProfileRequest req, Resume resume) {
        Member member = findMember(memberId);
        Profile profile = assembleProfile(req, resume);

        enrichWithSkills(req, profile);
        enrichWithCertificates(req, profile);
        enrichWithDesiredJobs(req, profile);
        recommendCapabilities(profile);

        member.setProfile(profile);
        return MemberResponse.from(member);
    }

    /** 단일 회원 조회 */
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findMember(memberId));
    }

    /** 프로필 조회 */
    public ProfileResponse getProfile(Long memberId) {
        Profile profile = findMember(memberId).getProfile();
        if (profile == null) throw new EntityNotFoundException("프로필이 존재하지 않습니다. id=" + memberId);
        return ProfileResponse.from(profile);
    }

    @Transactional
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // 엔티티 생성 헬퍼
    // ────────────────────────────────────────────────────────────────────────────────

    /** Member 엔티티 조립 */
    private Member assembleMember(MemberRequest request) {
        Account account = createAccount(request.loginRequest());
        Address address = createAddress(request.addressRequest());

        return new Member(
                null,
                request.name(),
                "USER", // TODO: Enum 으로 치환 고려
                request.birthDate(),
                request.phoneNumber(),
                account,
                address,
                null,
                null,
                null
        );
    }

    /** Account 엔티티 생성 */
    private Account createAccount(LoginRequest req) {
        return new Account(null, req.email(), passwordEncoder.encode(req.password()), null, null);
    }

    /** Address 엔티티 생성 */
    private Address createAddress(AddressRequest req) {
        return new Address(null, req.address(), req.addressJibun(), req.addressDetail(), req.regionCity(), req.zonecode(), null);
    }

    /** Profile 엔티티 기본 뼈대 생성 */
    private Profile assembleProfile(ProfileRequest req, Resume resume) {
        // Profile 생성자 변경에 따라 profileSkills 필드가 초기화됨 (기존 skills 필드 -> profileSkills 필드)
        return new Profile(
                null,
                req.educationLevel().name(),
                new HashSet<>(), // desiredJobs
                new HashSet<>(), // certificates
                new HashSet<>(), // profileSkills
                new HashSet<>(), // desiredCapabilities
                new HashSet<>(), // userCapabilities
                resume
        );
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // 프로필 확장(Enrich) 헬퍼
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * 기술 스킬 추가
     * SkillRequest(name, proficiency)를 받아 ProfileSkill 엔티티를 생성하고 Profile에 추가합니다.
     */
    private void enrichWithSkills(ProfileRequest req, Profile profile) {
        profile.getProfileSkills().clear(); // 기존 스킬 정보 초기화
        if (CollectionUtils.isEmpty(req.skills())) return;

        Set<ProfileSkill> profileSkills = req.skills().stream()
                .map(skillRequest -> {
                    // DB에 스킬이 없으면 새로 저장하고, 있으면 가져옵니다.
                    Skill skill = skillRepository.findByName(skillRequest.name())
                            .orElseGet(() -> skillRepository.save(new Skill(null, skillRequest.name())));

                    // Profile, Skill, Proficiency 정보를 담은 ProfileSkill 엔티티를 생성합니다.
                    return new ProfileSkill(profile, skill, skillRequest.proficiency());
                })
                .collect(Collectors.toSet());

        profile.getProfileSkills().addAll(profileSkills);
    }

    /** 자격증 추가 */
    private void enrichWithCertificates(ProfileRequest req, Profile profile) {
        profile.getProfileCertificates().clear();
        if (CollectionUtils.isEmpty(req.certificates())) return;

        Set<ProfileCertificate> pcs = req.certificates().stream()
                .map(c -> profileCertificateOf(profile, c.name(), c.year()))
                .collect(Collectors.toSet());
        profile.getProfileCertificates().addAll(pcs);
    }

    private ProfileCertificate profileCertificateOf(Profile profile, String certName, String year) {
        Certificate cert = certificateRepository.findByJmfldnm(certName)
                .orElseThrow(() -> new IllegalArgumentException("자격증을 찾을 수 없습니다: " + certName));
        return new ProfileCertificate(profile, cert, year);
    }

    /** 희망 직무 추가 */
    private void enrichWithDesiredJobs(ProfileRequest req, Profile profile) {
        if (CollectionUtils.isEmpty(req.desiredJobCodes())) return;

        Set<SaraminJob> jobs = req.desiredJobCodes().stream()
                .map(code -> saraminJobRepository.findById(code)
                        .orElseThrow(() -> new IllegalArgumentException("직무 코드가 존재하지 않습니다: " + code)))
                .collect(Collectors.toSet());
        profile.getDesiredJobs().addAll(jobs);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // 역량(NCS) 추천 로직
    // ────────────────────────────────────────────────────────────────────────────────

    private void recommendCapabilities(Profile profile) {
        recommendUserCapabilities(profile);
        recommendDesiredCapabilities(profile);
    }

    /**
     * 사용자 보유 역량 추천
     * profile.getSkills() 대신 profile.getProfileSkills()에서 기술 정보를 가져옵니다.
     */
    private void recommendUserCapabilities(Profile profile) {
        // 조건문 변경: profile.getSkills() -> profile.getProfileSkills()
        if (profile.getProfileSkills().isEmpty() && profile.getProfileCertificates().isEmpty()) return;

        Set<String> rec = openAiService.recommendNcsCodeUsingAssistant(profile)
                .blockOptional().orElseGet(HashSet::new);

        if (!rec.isEmpty()) {
            Set<NcsOccupation> valid = ncsApiService.filterValidNcsCodes(rec);
            profile.getUserCapabilities().clear();
            profile.getUserCapabilities().addAll(valid);
        }
    }

    /** 희망 직무 기반 역량 추천 */
    private void recommendDesiredCapabilities(Profile profile) {
        if (profile.getDesiredJobs().isEmpty()) return;

        Set<String> names = profile.getDesiredJobs().stream().map(SaraminJob::getName).collect(Collectors.toSet());
        Set<String> rec = openAiService.recommendDesiredJobCodeUsingAssistant(String.join(", ", names))
                .blockOptional().orElseGet(HashSet::new);

        if (!rec.isEmpty()) {
            Set<NcsOccupation> valid = ncsApiService.filterValidNcsCodes(rec);
            profile.getDesiredCapabilities().clear();
            profile.getDesiredCapabilities().addAll(valid);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // 공통 유틸
    // ────────────────────────────────────────────────────────────────────────────────

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다. id=" + id));
    }
}
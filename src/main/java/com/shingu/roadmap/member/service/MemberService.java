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

        member.setProfile(profile); // cascade = ALL + orphanRemoval = true
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

    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // 엔티티 생성 헬퍼
    // ────────────────────────────────────────────────────────────────────────────────

    /** Member 엔티티 조립 — 도메인 @Builder 사용 */
    private Member assembleMember(MemberRequest request) {
        Account account = createAccount(request.loginRequest());
        Address address = createAddress(request.addressRequest());

        return Member.builder()
                .name(request.name())
                .role("USER") // TODO: Enum 치환 고려
                .birthDate(request.birthDate())
                .phoneNumber(request.phoneNumber())
                .account(account)
                .address(address)
                // profile, recommendedTrainings, refreshToken 은 필요 시 이후에 설정
                .build();
    }

    /** Account 엔티티 생성 — 도메인 @Builder 사용 */
    private Account createAccount(LoginRequest req) {
        return Account.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .build();
    }

    /** Address 엔티티 생성 — 도메인 @Builder 사용 */
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

    /** Profile 엔티티 기본 뼈대 생성 — 도메인 @Builder 사용 */
    private Profile assembleProfile(ProfileRequest req, Resume resume) {
        return Profile.builder()
                .educationLevel(req.educationLevel() != null ? req.educationLevel().name() : null)
                .desiredJobs(new HashSet<>())
                .profileCertificates(new HashSet<>())
                .profileSkills(new HashSet<>())
                .desiredCapabilities(new HashSet<>())
                .userCapabilities(new HashSet<>())
                .resume(resume)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // 프로필 확장(Enrich) 헬퍼
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * 기술 스킬 추가
     * - 반드시 Profile의 편의 메서드(addSkill)를 사용해 양방향 일관성 유지
     */
    private void enrichWithSkills(ProfileRequest req, Profile profile) {
        profile.getProfileSkills().clear(); // 기존 스킬 정보 초기화 (orphanRemoval)
        if (req == null || CollectionUtils.isEmpty(req.skills())) return;

        for (var skillReq : req.skills()) {
            Skill skill = skillRepository.findByName(skillReq.name())
                    .orElseGet(() -> skillRepository.save(Skill.builder().name(skillReq.name()).build()));

            ProfileSkill ps = ProfileSkill.of(profile, skill, skillReq.proficiency());
            profile.addSkill(ps); // 양방향 setProfile 보장
        }
    }

    /** 자격증 추가 — addCertificate로 양방향 일관성 유지 */
    private void enrichWithCertificates(ProfileRequest req, Profile profile) {
        profile.getProfileCertificates().clear(); // orphanRemoval
        if (req == null || CollectionUtils.isEmpty(req.certificates())) return;

        for (var certReq : req.certificates()) {
            ProfileCertificate pc = profileCertificateOf(profile, certReq.name(), certReq.year());
            profile.addCertificate(pc); // setProfile 보장
        }
    }

    private ProfileCertificate profileCertificateOf(Profile profile, String certName, String year) {
        Certificate cert = certificateRepository.findByJmfldnm(certName)
                .orElseThrow(() -> new IllegalArgumentException("자격증을 찾을 수 없습니다: " + certName));
        return ProfileCertificate.of(profile, cert, year);
    }

    /** 희망 직무 추가 */
    private void enrichWithDesiredJobs(ProfileRequest req, Profile profile) {
        if (req == null || CollectionUtils.isEmpty(req.desiredJobCodes())) return;

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

    /** 사용자 보유 역량 추천 */
    private void recommendUserCapabilities(Profile profile) {
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

    // ────────────────────────────────────────────────────────────────────────────────
    // 공통 유틸
    // ────────────────────────────────────────────────────────────────────────────────

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다. id=" + id));
    }
}
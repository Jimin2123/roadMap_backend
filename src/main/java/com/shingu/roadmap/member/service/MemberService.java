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
import com.shingu.roadmap.common.exception.CustomException;
import com.shingu.roadmap.common.exception.ErrorCode;
import com.shingu.roadmap.common.enums.MemberRole;
import com.shingu.roadmap.common.repository.CertificateRepository;
import com.shingu.roadmap.common.repository.SkillRepository;
import com.shingu.roadmap.member.domain.*;
import com.shingu.roadmap.member.dto.request.AddressRequest;
import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.resume.domain.*;
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
        if (memberRepository.findByAccountEmail(request.loginRequest().email()).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
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

        // 스킬/자격/희망직무/NCS 역량 보강
        enrichWithSkills(req, profile);
        enrichWithCertificates(req, profile);
        enrichWithDesiredJobs(req, profile);
        recommendCapabilities(profile);

        // AI
        openAiService.recommendSearchCodes(profile)
                .timeout(Duration.ofSeconds(8))
                .onErrorResume(e -> Mono.empty())
                .blockOptional()
                .ifPresent(codes -> {
                    profile.updateRecommendedJobInfoCategoryCode(codes.get("jobInfoCategoryCode"));
                    profile.updateRecommendedJobInfoAbilityCode(codes.get("jobInfoAbilityCode")); // ← abilities에서 선택
                    profile.updateRecommendedEncyclopediaThemeCode(codes.get("encyclopediaThemeCode"));
                });

        member.setProfile(profile);
        return MemberResponse.from(member);
    }

    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findMember(memberId));
    }

    public ProfileResponse getProfile(Long memberId) {
        Profile profile = findMember(memberId).getProfile();
        if (profile == null) {
            throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        }
        return ProfileResponse.from(profile);
    }

    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
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
                .email(req.email())
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
                .desiredJobs(new HashSet<>())
                .profileCertificates(new HashSet<>())
                .profileSkills(new HashSet<>())
                .desiredCapabilities(new HashSet<>())
                .userCapabilities(new HashSet<>())
                .resume(resume) // ← Resume를 통째로 세팅(도메인 내부 add*/set* 사용해 조립해 들어온 객체)
                .build();
    }

    /* ====================================================================== */
    /* Resume Builders (도메인 일관성 준수)                                    */
    /* ====================================================================== */

    /** Period 안전 생성 */
    private Period periodOf(LocalDate start, LocalDate end) {
        return Period.of(start, end);
    }

    /** Introduction 생성/갱신 */
    public Introduction buildIntroduction(String content) {
        return Introduction.builder().content(content).build();
    }

    /** Education 생성/갱신 */
    public Education buildEducation(String school, String major, LocalDate start, LocalDate end, String status) {
        return Education.builder()
                .school(school)
                .major(major)
                .period(periodOf(start, end))
                .status(status)
                .build();
    }

    /** Activity 생성 */
    public Activity buildActivity(String title, String organization, LocalDate start, LocalDate end, String description) {
        return Activity.builder()
                .title(title)
                .organization(organization)
                .period(periodOf(start, end))
                .description(description)
                .build();
    }

    /** Project 생성 (성과/스택은 별도 attach 메서드로 연결 권장) */
    public Project buildProject(String name, String url, String role, String description, LocalDate start, LocalDate end) {
        return Project.builder()
                .name(name)
                .url(url)
                .role(role)
                .description(description)
                .period(periodOf(start, end))
                .build();
    }

    /** Resume 구성요소 연결: 기존 요소 초기화 후 add* 사용 (orphanRemoval 작동) */
    public Resume attachResumeParts(
            Resume resume,
            Introduction intro,                    // null 허용(미설정)
            Education education,                   // null 허용
            List<Activity> activities,             // null 가능
            List<Project> projects                 // null 가능
    ) {
        if (resume == null) resume = Resume.builder().build();

        // 단방향 1:1
        resume.setIntroduction(intro);
        resume.setEducation(education);

        // 1:N: orphanRemoval을 위해 교체 패턴 사용
        if (resume.getActivities() != null) resume.getActivities().clear();
        if (!CollectionUtils.isEmpty(activities)) {
            for (Activity a : activities) resume.addActivity(a);
        }

        if (resume.getProjects() != null) resume.getProjects().clear();
        if (!CollectionUtils.isEmpty(projects)) {
            for (Project p : projects) resume.addProject(p);
        }

        return resume;
    }

    /** Project에 성과/기술스택 연결 (양방향 없는 ManyToMany/ElementCollection) */
    public void attachProjectExtras(Project project, List<String> achievements, List<String> techSkillNames) {
        if (project == null) return;

        if (project.getAchievements() != null) project.getAchievements().clear();
        if (!CollectionUtils.isEmpty(achievements)) {
            achievements.forEach(project::addAchievement);
        }

        if (project.getTechStack() != null) project.getTechStack().clear();
        if (!CollectionUtils.isEmpty(techSkillNames)) {
            Set<Skill> stack = techSkillNames.stream()
                    .map(name -> skillRepository.findByName(name)
                            .orElseGet(() -> skillRepository.save(Skill.builder().name(name).build())))
                    .collect(Collectors.toSet());
            project.getTechStack().addAll(stack);
        }
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

    private void enrichWithCertificates(ProfileRequest req, Profile profile) {
        profile.getProfileCertificates().clear();
        if (req == null || CollectionUtils.isEmpty(req.certificates())) return;

        for (var certReq : req.certificates()) {
            ProfileCertificate pc = profileCertificateOf(profile, certReq.name(), certReq.year());
            profile.addCertificate(pc);
        }
    }

    private ProfileCertificate profileCertificateOf(Profile profile, String certName, String year) {
        Certificate cert = certificateRepository.findByJmfldnm(certName)
                .orElseThrow(() -> new CustomException(ErrorCode.CERTIFICATE_NOT_FOUND, "자격증을 찾을 수 없습니다: " + certName));
        return ProfileCertificate.of(profile, cert, year);
    }

    private void enrichWithDesiredJobs(ProfileRequest req, Profile profile) {
        if (req == null || CollectionUtils.isEmpty(req.desiredJobCodes())) return;

        Set<SaraminJob> jobs = req.desiredJobCodes().stream()
                .map(code -> saraminJobRepository.findById(code)
                        .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND, "직무 코드가 존재하지 않습니다: " + code)))
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
        if (profile.getProfileSkills().isEmpty() && profile.getProfileCertificates().isEmpty()) return;

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
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
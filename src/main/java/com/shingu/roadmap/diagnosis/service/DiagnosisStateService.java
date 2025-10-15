package com.shingu.roadmap.diagnosis.service;

import com.shingu.roadmap.diagnosis.domain.DiagnosisResult;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.dto.internal.MemberWithProfile;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.exception.ProfileNotFoundException;
import com.shingu.roadmap.diagnosis.repository.DiagnosisResultRepository;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.resume.domain.Education;
import com.shingu.roadmap.resume.domain.Introduction;
import com.shingu.roadmap.resume.domain.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 진단 상태 관리 및 데이터 영속성을 처리하는 서비스입니다.
 * Spring AOP 프록시를 통한 트랜잭션 적용을 보장하기 위해 DiagnosisService로부터 분리되었습니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisStateService {

    private final DiagnosisResultRepository diagnosisResultRepository;
    private final MemberRepository memberRepository;

    /**
     * 사용자 입력 후 진단을 재개하고 회원 ID를 반환합니다.
     *
     * @param diagnosisId 진단 ID
     * @return 회원 ID
     * @throws IllegalArgumentException 진단 정보가 없는 경우
     */
    @Transactional
    public Long resumeDiagnosisAfterUserInput(Long diagnosisId) {
        DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                .orElseThrow(() -> new IllegalArgumentException("진단 정보를 찾을 수 없습니다. diagnosisId: " + diagnosisId));

        Long memberId = diagnosisResult.getMemberId();
        log.info("Finding memberId for diagnosisId: {}", diagnosisId);

        // 진단 상태를 IN_PROGRESS로 변경 (도메인 메서드 사용)
        log.info("Updating diagnosis status for diagnosisId: {} to IN_PROGRESS", diagnosisId);
        if (diagnosisResult.getStatus() == DiagnosisStatus.PENDING) {
            diagnosisResult.startDiagnosis();
        } else if (diagnosisResult.getStatus() == DiagnosisStatus.AWAITING_USER_INPUT) {
            diagnosisResult.resumeDiagnosis();
        }
        diagnosisResultRepository.save(diagnosisResult);

        return memberId;
    }

    /**
     * 진단에 필요한 모든 데이터를 트랜잭션 내에서 완전히 로딩합니다.
     * 모든 lazy 컬렉션을 강제 초기화하여 @Async 메서드에서 LazyInitializationException을 방지합니다.
     *
     * @param memberId 회원 ID
     * @return 완전히 초기화된 Member와 Profile 데이터
     * @throws IllegalArgumentException Member 또는 Profile이 없는 경우
     * @throws ProfileNotFoundException Profile이 없는 경우
     */
    @Transactional(readOnly = true)
    public MemberWithProfile loadDiagnosisDataDetached(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for memberId: " + memberId));

        Profile profile = member.getProfile();
        if (profile == null) {
            throw new ProfileNotFoundException(memberId);
        }

        // Profile 컬렉션 초기화
        if (profile.getProfileSkills() != null) {
            Objects.hashCode(profile.getProfileSkills().size());
            // Skill 엔티티 내부 필드도 초기화
            profile.getProfileSkills().forEach(ps -> {
                if (ps.getSkill() != null) {
                    Objects.hashCode(ps.getSkill().getName());
                }
            });
        }

        if (profile.getDesiredJobs() != null) {
            Objects.hashCode(profile.getDesiredJobs().size());
        }

        if (profile.getDesiredCapabilities() != null) {
            Objects.hashCode(profile.getDesiredCapabilities().size());
        }

        if (profile.getUserCapabilities() != null) {
            Objects.hashCode(profile.getUserCapabilities().size());
        }

        // Resume 및 모든 중첩 컬렉션 초기화
        if (profile.getResume() != null) {
            Resume resume = profile.getResume();
            Objects.hashCode(resume.getId()); // Resume 자체 초기화

            // Introduction 초기화 (모든 @Lob 필드 강제 로딩)
            if (resume.getIntroduction() != null) {
                Introduction intro = resume.getIntroduction();
                Objects.hashCode(intro.getId());
                // @Lob 필드들을 강제로 초기화 (프록시 해제)
                if (intro.getGrowthProcess() != null) {
                    Objects.hashCode(intro.getGrowthProcess().length());
                }
                if (intro.getStrengths() != null) {
                    Objects.hashCode(intro.getStrengths().length());
                }
                if (intro.getSchoolLife() != null) {
                    Objects.hashCode(intro.getSchoolLife().length());
                }
                if (intro.getMotivation() != null) {
                    Objects.hashCode(intro.getMotivation().length());
                }
            }

            // Education 초기화 (모든 필드 강제 로딩)
            if (resume.getEducation() != null) {
                Education edu = resume.getEducation();
                Objects.hashCode(edu.getId());
                // 모든 필드를 강제로 초기화 (프록시 해제)
                if (edu.getSchool() != null) {
                    Objects.hashCode(edu.getSchool().length());
                }
                if (edu.getMajor() != null) {
                    Objects.hashCode(edu.getMajor().length());
                }
                if (edu.getGpa() != null) {
                    Objects.hashCode(edu.getGpa());
                }
                if (edu.getStatus() != null) {
                    Objects.hashCode(edu.getStatus().length());
                }
                if (edu.getPeriod() != null) {
                    Objects.hashCode(edu.getPeriod().getStartDate());
                    if (edu.getPeriod().getEndDate() != null) {
                        Objects.hashCode(edu.getPeriod().getEndDate());
                    }
                }
            }

            // DesiredCompany 초기화
            if (resume.getDesiredCompany() != null) {
                Objects.hashCode(resume.getDesiredCompany().getId());
            }

            // Activities 컬렉션 초기화 (모든 필드 강제 로딩)
            if (resume.getActivities() != null) {
                Objects.hashCode(resume.getActivities().size());
                resume.getActivities().forEach(activity -> {
                    // Activity의 모든 필드 초기화
                    if (activity.getTitle() != null) {
                        Objects.hashCode(activity.getTitle().length());
                    }
                    if (activity.getOrganization() != null) {
                        Objects.hashCode(activity.getOrganization().length());
                    }
                    if (activity.getDescription() != null) {
                        Objects.hashCode(activity.getDescription().length());
                    }
                    if (activity.getPeriod() != null) {
                        Objects.hashCode(activity.getPeriod().getStartDate());
                        if (activity.getPeriod().getEndDate() != null) {
                            Objects.hashCode(activity.getPeriod().getEndDate());
                        }
                    }
                });
            }

            // Projects 컬렉션 초기화 (모든 필드 강제 로딩)
            if (resume.getProjects() != null) {
                Objects.hashCode(resume.getProjects().size());
                resume.getProjects().forEach(project -> {
                    // Project의 모든 필드 초기화
                    if (project.getName() != null) {
                        Objects.hashCode(project.getName().length());
                    }
                    if (project.getRole() != null) {
                        Objects.hashCode(project.getRole().length());
                    }
                    if (project.getDescription() != null) {
                        Objects.hashCode(project.getDescription().length());
                    }
                    if (project.getUrl() != null) {
                        Objects.hashCode(project.getUrl().length());
                    }
                    if (project.getPeriod() != null) {
                        Objects.hashCode(project.getPeriod().getStartDate());
                        if (project.getPeriod().getEndDate() != null) {
                            Objects.hashCode(project.getPeriod().getEndDate());
                        }
                    }
                    // TechStack 컬렉션 초기화
                    if (project.getTechStack() != null) {
                        Objects.hashCode(project.getTechStack().size());
                        project.getTechStack().forEach(skill -> {
                            if (skill != null) {
                                Objects.hashCode(skill.getName());
                            }
                        });
                    }
                    // Achievements 컬렉션 초기화
                    if (project.getAchievements() != null) {
                        Objects.hashCode(project.getAchievements().size());
                        // List<String>이므로 각 String도 초기화
                        project.getAchievements().forEach(achievement -> {
                            if (achievement != null) {
                                Objects.hashCode(achievement.length());
                            }
                        });
                    }
                });
            }

            // Careers 컬렉션 초기화 (모든 필드 강제 로딩)
            if (resume.getCareers() != null) {
                Objects.hashCode(resume.getCareers().size());
                resume.getCareers().forEach(career -> {
                    // Career의 모든 필드 초기화
                    if (career.getCompanyName() != null) {
                        Objects.hashCode(career.getCompanyName().getValue().length());
                    }
                    if (career.getDepartment() != null) {
                        Objects.hashCode(career.getDepartment().length());
                    }
                    if (career.getDescription() != null) {
                        Objects.hashCode(career.getDescription().length());
                    }
                    if (career.getPeriod() != null) {
                        Objects.hashCode(career.getPeriod().getStartDate());
                        if (career.getPeriod().getEndDate() != null) {
                            Objects.hashCode(career.getPeriod().getEndDate());
                        }
                    }
                });
            }

            // Certificates 컬렉션 초기화
            if (resume.getCertificates() != null) {
                Objects.hashCode(resume.getCertificates().size());
                resume.getCertificates().forEach(cert -> {
                    if (cert.getCertificate() != null) {
                        Objects.hashCode(cert.getCertificate().getJmfldnm());
                    }
                });
            }
        }

        log.debug("Successfully loaded and initialized all diagnosis data for memberId: {}", memberId);
        return new MemberWithProfile(member, profile);
    }

    /**
     * 진단 결과를 트랜잭션 내에서 저장합니다.
     *
     * @param diagnosisId 진단 ID
     * @param response 진단 결과 응답
     * @throws IllegalArgumentException 진단 정보가 없는 경우
     */
    @Transactional
    public void saveDiagnosisResultData(Long diagnosisId, DiagnosisResultResponse response) {
        log.info("Saving diagnosis result for diagnosisId: {}", diagnosisId);

        DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                .orElseThrow(() -> new IllegalArgumentException("진단 정보를 찾을 수 없습니다. diagnosisId: " + diagnosisId));

        // DiagnosisResultResponse를 DiagnosisResultData Value Object로 변환
        com.shingu.roadmap.diagnosis.domain.DiagnosisResultData resultData =
                com.shingu.roadmap.diagnosis.domain.DiagnosisResultData.fromResponse(
                        response.summary(),
                        response.ncsAnalyses(),
                        response.confidenceScore(),
                        response.radarChartData()
                );

        // 진단 완료 (도메인 메서드 사용)
        diagnosisResult.completeDiagnosis(resultData);
        diagnosisResultRepository.save(diagnosisResult);

        log.info("Diagnosis result saved to database for diagnosisId: {}", diagnosisId);
    }

    /**
     * 진단 실패를 트랜잭션 내에서 기록합니다.
     *
     * @param diagnosisId 진단 ID
     * @param errorMessage 오류 메시지
     */
    @Transactional
    public void failDiagnosisWithError(Long diagnosisId, String errorMessage) {
        try {
            DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                    .orElseThrow(() -> new IllegalArgumentException("진단 정보를 찾을 수 없습니다. diagnosisId: " + diagnosisId));

            diagnosisResult.failDiagnosis(errorMessage);
            diagnosisResultRepository.save(diagnosisResult);

            log.info("Diagnosis marked as failed for diagnosisId: {}", diagnosisId);
        } catch (Exception e) {
            log.error("Failed to mark diagnosis as failed for diagnosisId: {}", diagnosisId, e);
        }
    }
}

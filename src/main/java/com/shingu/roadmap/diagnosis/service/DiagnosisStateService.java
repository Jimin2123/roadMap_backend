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

        if (profile.getDesiredCapabilities() != null) {
            Objects.hashCode(profile.getDesiredCapabilities().size());
        }

        if (profile.getUserCapabilities() != null) {
            Objects.hashCode(profile.getUserCapabilities().size());
        }

        // Resume 및 모든 중첩 컬렉션 초기화
        if (profile.getResume() != null) {
            var resume = profile.getResume();
            Objects.hashCode(resume.getId()); // Resume 자체 초기화

            // Introduction 초기화
            if (resume.getIntroduction() != null) {
                Objects.hashCode(resume.getIntroduction().getId());
            }

            // Education 초기화
            if (resume.getEducation() != null) {
                Objects.hashCode(resume.getEducation().getId());
            }

            // DesiredCompany 초기화
            if (resume.getDesiredCompany() != null) {
                Objects.hashCode(resume.getDesiredCompany().getId());
            }

            // Activities 컬렉션 초기화
            if (resume.getActivities() != null) {
                Objects.hashCode(resume.getActivities().size());
                resume.getActivities().forEach(activity -> {
                    if (activity.getPeriod() != null) {
                        Objects.hashCode(activity.getPeriod().getStartDate());
                    }
                });
            }

            // Projects 컬렉션 초기화
            if (resume.getProjects() != null) {
                Objects.hashCode(resume.getProjects().size());
                resume.getProjects().forEach(project -> {
                    Objects.hashCode(project.getName());
                    if (project.getPeriod() != null) {
                        Objects.hashCode(project.getPeriod().getStartDate());
                    }
                });
            }

            // Careers 컬렉션 초기화
            if (resume.getCareers() != null) {
                Objects.hashCode(resume.getCareers().size());
                resume.getCareers().forEach(career -> {
                    if (career.getPeriod() != null) {
                        Objects.hashCode(career.getPeriod().getStartDate());
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

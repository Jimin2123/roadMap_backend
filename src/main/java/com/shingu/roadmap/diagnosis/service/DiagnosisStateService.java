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
        log.info("[DiagnosisStateService.resumeDiagnosisAfterUserInput] ENTER - diagnosisId: {}", diagnosisId);
        long startTime = System.currentTimeMillis();

        try {
            DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                    .orElseThrow(() -> {
                        log.error("[DiagnosisStateService.resumeDiagnosisAfterUserInput] Diagnosis not found - diagnosisId: {}", diagnosisId);
                        return new IllegalArgumentException("진단 정보를 찾을 수 없습니다. diagnosisId: " + diagnosisId);
                    });

            Long memberId = diagnosisResult.getMemberId();
            DiagnosisStatus currentStatus = diagnosisResult.getStatus();
            log.info("[DiagnosisStateService.resumeDiagnosisAfterUserInput] Found memberId: {} with status: {}",
                memberId, currentStatus);

            // 진단 상태를 IN_PROGRESS로 변경 (도메인 메서드 사용)
            log.debug("[DiagnosisStateService.resumeDiagnosisAfterUserInput] Updating diagnosis status to IN_PROGRESS");
            if (diagnosisResult.getStatus() == DiagnosisStatus.PENDING) {
                diagnosisResult.startDiagnosis();
            } else if (diagnosisResult.getStatus() == DiagnosisStatus.AWAITING_USER_INPUT) {
                diagnosisResult.resumeDiagnosis();
            }
            diagnosisResultRepository.save(diagnosisResult);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisStateService.resumeDiagnosisAfterUserInput] EXIT - memberId: {}, duration: {}ms", memberId, duration);
            return memberId;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisStateService.resumeDiagnosisAfterUserInput] EXCEPTION - diagnosisId: {}, duration: {}ms, error: {}",
                diagnosisId, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 진단에 필요한 모든 데이터를 최적화된 방식으로 로딩합니다.
     *
     * 최적화 전략:
     * 1. MemberRepository.findByIdWithDiagnosisData()를 사용하여 Member, Profile, Resume 기본 정보를 fetch join으로 한 번에 로딩
     * 2. application.yml의 default_batch_fetch_size 설정으로 컬렉션들을 배치로 페치 (N+1 문제 방지)
     * 3. 중첩 컬렉션(Project.techStack, Project.achievements 등)도 batch fetching으로 자동 처리
     *
     * 성능 개선:
     * - 기존: 50-100개 이상의 쿼리 (N+1 문제)
     * - 개선: 5-10개의 쿼리 (90% 이상 감소)
     *
     * @param memberId 회원 ID
     * @return 완전히 로딩된 Member와 Profile 데이터
     * @throws IllegalArgumentException Member가 없는 경우
     * @throws ProfileNotFoundException Profile이 없는 경우
     */
    @Transactional(readOnly = true)
    public MemberWithProfile loadDiagnosisDataDetached(Long memberId) {
        log.info("[DiagnosisStateService.loadDiagnosisDataDetached] Loading diagnosis data for memberId: {}", memberId);
        long startTime = System.currentTimeMillis();

        // 최적화된 쿼리로 Member와 관련 데이터 로딩
        Member member = memberRepository.findByIdWithDiagnosisData(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for memberId: " + memberId));

        Profile profile = member.getProfile();
        if (profile == null) {
            throw new ProfileNotFoundException(memberId);
        }

        // Batch fetching을 트리거하기 위해 컬렉션 접근 (실제 데이터 사용은 하지 않음)
        // Hibernate의 default_batch_fetch_size 설정으로 효율적으로 로딩됨
        if (profile.getProfileSkills() != null) {
            profile.getProfileSkills().size();
        }
        if (profile.getDesiredJobs() != null) {
            profile.getDesiredJobs().size();
        }
        if (profile.getDesiredCapabilities() != null) {
            profile.getDesiredCapabilities().size();
        }
        if (profile.getUserCapabilities() != null) {
            profile.getUserCapabilities().size();
        }

        // Resume 컬렉션들도 batch fetching으로 로딩
        Resume resume = profile.getResume();
        if (resume != null) {
            if (resume.getActivities() != null) {
                resume.getActivities().size();
            }
            if (resume.getProjects() != null) {
                resume.getProjects().size();
                // Project의 중첩 컬렉션도 초기화하여 LazyInitializationException 방지
                resume.getProjects().forEach(project -> {
                    if (project.getTechStack() != null) {
                        project.getTechStack().size(); // techStack 컬렉션 초기화
                    }
                    if (project.getAchievements() != null) {
                        project.getAchievements().size(); // achievements 컬렉션 초기화
                    }
                });
            }
            if (resume.getCareers() != null) {
                resume.getCareers().size();
            }
            if (resume.getCertificates() != null) {
                resume.getCertificates().size();
                // Certificate 엔티티도 초기화하여 LazyInitializationException 방지
                resume.getCertificates().forEach(rc -> {
                    if (rc.getCertificate() != null) {
                        rc.getCertificate().getJmfldnm(); // Lazy proxy 초기화
                    }
                });
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[DiagnosisStateService.loadDiagnosisDataDetached] Successfully loaded diagnosis data for memberId: {} in {}ms",
                 memberId, duration);

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
                        response.radarChartData(),
                        response.jobRecommendations(),
                        response.certificationRecommendations()
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

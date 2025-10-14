package com.shingu.roadmap.diagnosis.repository;

import com.shingu.roadmap.diagnosis.domain.DiagnosisResult;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 진단 결과 리포지토리
 */
@Repository
public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Long> {

    /**
     * 회원 ID로 가장 최근 진단 결과 조회
     *
     * @param memberId 회원 ID
     * @return 가장 최근 진단 결과
     */
    Optional<DiagnosisResult> findFirstByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 회원 ID로 모든 진단 결과 조회 (최신순)
     *
     * @param memberId 회원 ID
     * @return 진단 결과 목록
     */
    List<DiagnosisResult> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 회원 ID와 상태로 진단 결과 조회
     *
     * @param memberId 회원 ID
     * @param status 진단 상태
     * @return 진단 결과 목록
     */
    List<DiagnosisResult> findByMemberIdAndStatus(Long memberId, DiagnosisStatus status);

    /**
     * 회원 ID와 상태로 진행 중인 진단 조회 (단일 결과)
     *
     * @param memberId 회원 ID
     * @param status 진단 상태
     * @return 진행 중인 진단 (없으면 Optional.empty())
     */
    Optional<DiagnosisResult> findFirstByMemberIdAndStatus(Long memberId, DiagnosisStatus status);

    /**
     * 특정 회원의 완료된 진단 결과 개수 조회
     *
     * @param memberId 회원 ID
     * @param status 진단 상태
     * @return 진단 결과 개수
     */
    long countByMemberIdAndStatus(Long memberId, DiagnosisStatus status);
}

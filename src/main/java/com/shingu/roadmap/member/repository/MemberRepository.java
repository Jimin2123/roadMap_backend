package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Member;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
  @Query("SELECT m FROM Member m JOIN FETCH m.account WHERE m.account.email.value = :email")
  Optional<Member> findByAccountEmail(@Param("email") String email);

  @EntityGraph(value = "Member.withAccountProfileAddress", type = EntityGraph.EntityGraphType.LOAD)
  Optional<Member> findById(Long id);

  Optional<Member> findByRefreshToken_Token(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM Member m WHERE m.refreshToken.token = :token")
  Optional<Member> findAndLockByRefreshToken_Token(@Param("token") String token);

  /**
   * 진단(Diagnosis)에 필요한 모든 Member 데이터를 최적화된 방식으로 로딩합니다.
   *
   * 전략:
   * 1. Member, Profile, Resume의 기본 정보를 fetch join으로 로딩
   * 2. ProfileSkills와 Skill을 함께 로딩하여 LazyInitializationException 방지
   * 3. 나머지 컬렉션은 batch fetching으로 처리 (application.yml의 default_batch_fetch_size 사용)
   *
   * 주의: 여러 컬렉션을 동시에 fetch join하면 cartesian product가 발생할 수 있으므로
   * DISTINCT를 사용하고, 가장 중요한 profileSkills만 fetch join으로 처리합니다.
   *
   * @param memberId 회원 ID
   * @return 완전히 로딩된 Member 엔티티
   */
  @Query("SELECT DISTINCT m FROM Member m " +
         "LEFT JOIN FETCH m.profile p " +
         "LEFT JOIN FETCH p.resume r " +
         "LEFT JOIN FETCH r.introduction " +
         "LEFT JOIN FETCH r.education " +
         "LEFT JOIN FETCH r.desiredCompany " +
         "LEFT JOIN FETCH p.profileSkills ps " +
         "LEFT JOIN FETCH ps.skill " +
         "WHERE m.id = :memberId")
  Optional<Member> findByIdWithDiagnosisData(@Param("memberId") Long memberId);

  /**
   * Profile 조회에 필요한 모든 연관 데이터를 한 번에 로딩합니다. (N+1 쿼리 방지)
   *
   * 전략:
   * 1. 첫 번째 쿼리: Member, Profile, ProfileSkills, Skill을 fetch join
   * 2. 두 번째 쿼리: desiredJobs, desiredCapabilities, userCapabilities를 batch fetch
   *
   * 주의: 여러 컬렉션을 동시에 fetch join하면 cartesian product 발생
   * DISTINCT를 사용하여 중복 제거
   *
   * @param memberId 회원 ID
   * @return Profile 응답에 필요한 모든 데이터가 로딩된 Member
   */
  @Query("SELECT DISTINCT m FROM Member m " +
         "LEFT JOIN FETCH m.profile p " +
         "LEFT JOIN FETCH p.profileSkills ps " +
         "LEFT JOIN FETCH ps.skill " +
         "WHERE m.id = :memberId")
  Optional<Member> findByIdWithProfile(@Param("memberId") Long memberId);
}

package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
  @Query("SELECT m FROM Member m JOIN FETCH m.account WHERE m.account.email = :email")
  Optional<Member> findByAccountEmail(@Param("email") String email);

  @EntityGraph(value = "Member.withAccountProfileAddress", type = EntityGraph.EntityGraphType.LOAD)
  Optional<Member> findById(Long id);

  Optional<Member> findByRefreshToken_Token(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM Member m WHERE m.refreshToken.token = :token")
  Optional<Member> findAndLockByRefreshToken_Token(@Param("token") String token);
}

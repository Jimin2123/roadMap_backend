package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
  @Query("SELECT m FROM Member m JOIN FETCH m.account WHERE m.account.email = :email")
  Optional<Member> findByAccountEmail(@Param("email") String email);
}

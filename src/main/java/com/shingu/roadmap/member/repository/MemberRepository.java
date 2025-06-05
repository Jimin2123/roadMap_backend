package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByAccountEmail(String email);
}

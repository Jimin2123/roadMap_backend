package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

}

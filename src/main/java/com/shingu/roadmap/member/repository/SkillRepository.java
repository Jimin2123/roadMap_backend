package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
  Optional<Skill> findByName(String skillName);
}

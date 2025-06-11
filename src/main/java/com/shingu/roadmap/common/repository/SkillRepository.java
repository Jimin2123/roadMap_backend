package com.shingu.roadmap.common.repository;

import com.shingu.roadmap.common.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
  Optional<Skill> findByName(String skillName);
  List<Skill> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String query);
}

package com.shingu.roadmap.common.repository;

import com.shingu.roadmap.common.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SkillRepository extends JpaRepository<Skill, Long> {
  Optional<Skill> findByName(String skillName);
  List<Skill> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String query);

  /**
   * 여러 스킬 이름으로 배치 조회 (N+1 쿼리 방지)
   *
   * @param names 조회할 스킬 이름 목록
   * @return 해당 이름을 가진 Skill 엔티티 리스트
   */
  @Query("SELECT s FROM Skill s WHERE s.name IN :names")
  List<Skill> findAllByNameIn(@Param("names") Set<String> names);
}

package com.shingu.roadmap.resume.repository;

import com.shingu.roadmap.resume.domain.Resume;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
  @EntityGraph(value = "Resume.withAll", type = EntityGraph.EntityGraphType.LOAD)
  Optional<Resume> findById(Long id);
}

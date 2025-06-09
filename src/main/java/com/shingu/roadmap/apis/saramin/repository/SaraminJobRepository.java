package com.shingu.roadmap.apis.saramin.repository;

import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaraminJobRepository extends JpaRepository<SaraminJob, Integer> {
  Optional<SaraminJob> findByName(String name);
}
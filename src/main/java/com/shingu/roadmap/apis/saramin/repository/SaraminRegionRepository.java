package com.shingu.roadmap.apis.saramin.repository;

import com.shingu.roadmap.apis.saramin.domain.SaraminRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaraminRegionRepository extends JpaRepository<SaraminRegion, Integer> {

  Optional<SaraminRegion> findFirstByName(String name);
}
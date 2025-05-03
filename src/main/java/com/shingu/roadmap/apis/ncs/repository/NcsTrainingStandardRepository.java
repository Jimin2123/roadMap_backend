package com.shingu.roadmap.apis.ncs.repository;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.domain.NcsTrainingStandard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NcsTrainingStandardRepository extends JpaRepository<NcsTrainingStandard, Long> {
  Optional<NcsTrainingStandard> findByItemCdAndItemNo(String itemCd, String itemNo);
}

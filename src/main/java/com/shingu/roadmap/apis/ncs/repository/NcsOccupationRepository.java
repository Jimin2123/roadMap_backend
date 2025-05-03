package com.shingu.roadmap.apis.ncs.repository;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NcsOccupationRepository extends JpaRepository<NcsOccupation, String> { }
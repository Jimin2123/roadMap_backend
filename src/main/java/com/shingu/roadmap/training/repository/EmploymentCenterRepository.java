package com.shingu.roadmap.training.repository;

import com.shingu.roadmap.apis.work24.domain.Work24EmploymentCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmploymentCenterRepository extends JpaRepository<Work24EmploymentCenter, String> {

  /* 고용센터(센터명)만 */
  @Query("select e from Work24EmploymentCenter e where e.regionOffice is not null and e.regionOffice <> ''")
  List<Work24EmploymentCenter> findAllCenters();

  /* 지방청(지방고용노동청)만 */
  @Query("select e from Work24EmploymentCenter e where e.centerName is not null and e.centerName <> ''")
  List<Work24EmploymentCenter> findAllOffices();

  boolean existsByCategoryId(String categoryId);
}

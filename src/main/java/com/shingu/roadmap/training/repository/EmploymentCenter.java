package com.shingu.roadmap.training.repository;

import com.shingu.roadmap.apis.work24.domain.Work24EmploymentCenter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentCenter extends JpaRepository<Work24EmploymentCenter, String> { }

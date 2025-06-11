package com.shingu.roadmap.resume.repository;

import com.shingu.roadmap.resume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> { }

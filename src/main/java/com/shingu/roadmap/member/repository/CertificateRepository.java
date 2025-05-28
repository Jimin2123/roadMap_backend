package com.shingu.roadmap.member.repository;

import com.shingu.roadmap.member.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
  Optional<Certificate> findByJmfldnm(String name);
}

package com.shingu.roadmap.common.repository;

import com.shingu.roadmap.common.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
  Optional<Certificate> findByJmfldnm(String name);

  List<Certificate> findTop10ByJmfldnmContainingIgnoreCaseOrderByJmfldnmAsc(String query);
}

package com.shingu.roadmap.auth.repository;

import com.shingu.roadmap.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);
  void deleteByToken(String token);

  // 선택: 운영 편의를 위한 만료 토큰 일괄 삭제
  long deleteByExpiresAtBefore(Instant cutoff);
}
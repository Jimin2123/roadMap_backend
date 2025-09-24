package com.shingu.roadmap.auth.repository;

import com.shingu.roadmap.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);

  /**
   * 동시 접근 제어를 위한 비관적 잠금 조회
   * Race Condition 방지를 위해 해당 토큰 레코드에 쓰기 락을 걸어
   * 한 번에 하나의 요청만 처리되도록 보장
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token")
  Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

  /**
   * 토큰 사용 후 즉시 삭제 (One-Time-Use 패턴)
   */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
  int deleteByTokenImmediate(@Param("token") String token);

  void deleteByToken(String token);

  // 선택: 운영 편의를 위한 만료 토큰 일괄 삭제
  long deleteByExpiresAtBefore(Instant cutoff);
}
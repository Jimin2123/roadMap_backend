package com.shingu.roadmap.apis.saramin.repository;

import com.shingu.roadmap.apis.saramin.domain.SaraminRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaraminRegionRepository extends JpaRepository<SaraminRegion, Integer> {

  Optional<SaraminRegion> findFirstByName(String name);

  /**
   * 지역명으로 검색 (LIKE 검색 지원)
   * 예: "서울" 검색 시 "서울특별시", "서울전체" 등 매칭
   */
  List<SaraminRegion> findByNameContaining(String name);

  /**
   * 1차 지역 코드로 모든 하위 지역 조회
   * 예: 101000 (서울전체) → 강남구, 강동구 등 모든 서울 하위 지역 반환
   */
  List<SaraminRegion> findByRegionCode1(Integer regionCode1);

  /**
   * 2차 지역 코드로 검색
   */
  Optional<SaraminRegion> findByRegionCode2(Integer regionCode2);

  /**
   * 지역명이 특정 키워드를 포함하는지 대소문자 무시하고 검색
   * 예: "서울", "Seoul", "seoul" 모두 매칭
   */
  @Query("SELECT sr FROM SaraminRegion sr WHERE LOWER(sr.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  List<SaraminRegion> findByNameContainingIgnoreCase(@Param("keyword") String keyword);

  /**
   * 특정 지역과 관련된 모든 지역 코드 조회 (1차 지역 기준)
   * 예: "서울특별시" 입력 시 서울 전체 지역 코드 반환
   */
  @Query("SELECT sr FROM SaraminRegion sr WHERE sr.regionCode1 = " +
         "(SELECT r.regionCode1 FROM SaraminRegion r WHERE r.name = :regionName)")
  List<SaraminRegion> findAllRelatedRegionsByName(@Param("regionName") String regionName);
}
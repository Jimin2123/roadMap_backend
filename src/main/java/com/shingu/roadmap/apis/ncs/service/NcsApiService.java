package com.shingu.roadmap.apis.ncs.service;

import com.shingu.roadmap.apis.ncs.client.NcsApiClient;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupationStandardLink;
import com.shingu.roadmap.apis.ncs.domain.NcsTrainingStandard;
import com.shingu.roadmap.apis.ncs.dto.response.*;
import com.shingu.roadmap.apis.ncs.repository.NcsOccupationRepository;
import com.shingu.roadmap.apis.ncs.repository.NcsTrainingStandardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NcsApiService {

  private final NcsApiClient ncsApiClient;
  private final NcsOccupationRepository ncsOccupationRepository;
  private final NcsTrainingStandardRepository ncsTrainingStandardRepository;

  // 병렬 처리를 위한 전용 스레드 풀
  private final Executor ncsProcessingExecutor = Executors.newFixedThreadPool(5);

  /**
   * NCS 코드 유효성 검사 및 등록 (병렬 처리 최적화)
   * @param ncsCodes
   * @return
   */
  public Set<NcsOccupation> filterValidNcsCodes(Set<String> ncsCodes) {
    if (ncsCodes == null || ncsCodes.isEmpty()) {
      return new HashSet<>();
    }

    // 1. 이미 DB에 존재하는 코드들 먼저 조회
    Map<String, NcsOccupation> existingOccupations = ncsOccupationRepository.findAllById(ncsCodes)
            .stream()
            .collect(Collectors.toMap(NcsOccupation::getDutyCd, occ -> occ));

    // 2. 존재하지 않는 코드들만 병렬로 처리
    Set<String> missingCodes = ncsCodes.stream()
            .filter(code -> !existingOccupations.containsKey(code))
            .collect(Collectors.toSet());

    if (missingCodes.isEmpty()) {
      return new HashSet<>(existingOccupations.values());
    }

    log.info("Processing {} missing NCS codes in parallel", missingCodes.size());

    // 3. 병렬로 API 호출 및 등록
    List<CompletableFuture<Optional<NcsOccupation>>> futures = missingCodes.stream()
            .map(ncsCode -> CompletableFuture
                    .supplyAsync(() -> {
                      try {
                        boolean success = fetchAndRegisterNcsOccupation(ncsCode);
                        return success ? ncsOccupationRepository.findById(ncsCode) : Optional.<NcsOccupation>empty();
                      } catch (Exception e) {
                        log.warn("Failed to process NCS code {}: {}", ncsCode, e.getMessage());
                        return Optional.<NcsOccupation>empty();
                      }
                    }, ncsProcessingExecutor))
            .collect(Collectors.toList());

    // 4. 모든 비동기 작업 완료 대기
    Set<NcsOccupation> validOccupations = new HashSet<>(existingOccupations.values());
    futures.forEach(future -> {
      try {
        future.join().ifPresent(validOccupations::add);
      } catch (Exception e) {
        log.warn("Failed to join future: {}", e.getMessage());
      }
    });

    log.info("Successfully processed {} out of {} NCS codes", validOccupations.size(), ncsCodes.size());
    return validOccupations;
  }

  /**
   * NCS 직무 정보 조회 및 등록 (캐싱 적용)
   * @param ncsCode
   * @return
   */
  @Cacheable(value = "ncsOccupation", key = "#ncsCode")
  public boolean fetchAndRegisterNcsOccupation(String ncsCode) {
    // 1. 직무 정보 조회
    NcsOccupationResponse response = ncsApiClient.getOccupation(ncsCode);
    List<NcsOccupationResponse.NcsOccupationItem> items = response.data();

    if (items == null || items.isEmpty()) return false;

    NcsOccupationResponse.NcsOccupationItem item = items.getFirst();

    // 2. 직무 도메인 생성
    NcsOccupation ncsOccupation = new NcsOccupation(
            item.dutyCd(),
            item.dutyNm(),
            item.dutySvcNo(),
            item.dutyDef()
    );

    // 3. 훈련기준 목록 조회
    List<NcsTrainingStandardResponse.NcsTrainingStandardItem> standardItems =
            ncsApiClient.getNcsTrainingStandard(ncsCode).data();

    if (standardItems == null) return false;

    for (NcsTrainingStandardResponse.NcsTrainingStandardItem stdItem : standardItems) {
      if ("관련 홈페이지 안내".equals(stdItem.itemName())) {
        continue;
      }

      // 4. 기준 항목 조회 또는 생성
      NcsTrainingStandard standard = ncsTrainingStandardRepository
              .findByDefText(stdItem.defText())
              .orElseGet(() -> {
                try {
                  NcsTrainingStandard newStandard = new NcsTrainingStandard(
                          stdItem.itemName(),
                          stdItem.defText()
                  );
                  return ncsTrainingStandardRepository.save(newStandard);
                } catch (DataIntegrityViolationException e) {
                  // 누군가 먼저 저장한 경우 → 다시 조회
                  return ncsTrainingStandardRepository.findByDefText(stdItem.defText())
                          .orElseThrow(() -> new IllegalStateException("중복 저장 후 재조회 실패"));
                }
              });
      // 5. 중복 연결 방지 및 양방향 관계 설정
      if (ncsOccupation.hasTrainingStandard(standard)) continue;

      NcsOccupationStandardLink link = new NcsOccupationStandardLink(ncsOccupation, standard);
      ncsOccupation.getTrainingLinks().add(link);
      standard.getOccupations().add(link);
    }

    // 6. 직무 정보 저장
    ncsOccupationRepository.save(ncsOccupation);
    return true;
  }

  /**
   * NCS 직책 조회
   *
   * @param ncsCode NCS 코드
   * @return NCS 직책 응답 DTO
   */
  public NcsJobPositionResponse getNcsJobPosition(String ncsCode) {
    return ncsApiClient.getNcsJobPosition(ncsCode);
  }

  /**
   * NCS 능력단위 조회
   *
   * @param ncsCode NCS 코드
   * @return NCS 능력단위 응답 DTO
   */
  public NcsCompUnitResponse getNcsCompUnit(String ncsCode) {
    return ncsApiClient.getNcsCompetencyUnit(ncsCode);
  }

  /**
   * NCS KSA 조회
   *
   * @param ncsCode    NCS 코드
   * @param compUnitCd 능력단위 코드
   * @return NCS KSA 응답 DTO
   */
  public NcsKsaResponse getNcsKsa(String ncsCode, String compUnitCd) {
    return ncsApiClient.getNcsKsaByDutyCode(ncsCode, compUnitCd);
  }
}

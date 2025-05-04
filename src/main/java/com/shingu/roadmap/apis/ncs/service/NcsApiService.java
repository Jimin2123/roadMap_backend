package com.shingu.roadmap.apis.ncs.service;

import com.shingu.roadmap.apis.ncs.client.NcsApiClient;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupationStandardLink;
import com.shingu.roadmap.apis.ncs.domain.NcsTrainingStandard;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsTrainingStandardResponse;
import com.shingu.roadmap.apis.ncs.repository.NcsOccupationRepository;
import com.shingu.roadmap.apis.ncs.repository.NcsTrainingStandardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NcsApiService {

  private final NcsApiClient ncsApiClient;
  private final NcsOccupationRepository ncsOccupationRepository;
  private final NcsTrainingStandardRepository ncsTrainingStandardRepository;

  /**
   * NCS 코드 유효성 검사 및 등록
   * @param ncsCodes
   * @return
   */
  public Set<NcsOccupation> filterValidNcsCodes(Set<String> ncsCodes) {
    Set<NcsOccupation> validOccupations = new HashSet<>();

    for (String ncsCode : ncsCodes) {
      // 이미 존재하면 조회해서 추가
      if (ncsOccupationRepository.existsById(ncsCode)) {
        ncsOccupationRepository.findById(ncsCode).ifPresent(validOccupations::add);
        continue;
      }

      // 존재하지 않으면 API 호출 및 등록 시도
      boolean success = fetchAndRegisterNcsOccupation(ncsCode);
      if (success) {
        ncsOccupationRepository.findById(ncsCode).ifPresent(validOccupations::add);
      }
    }

    return validOccupations;
  }

  /**
   * NCS 직무 정보 조회 및 등록
   * @param ncsCode
   * @return
   */
  public boolean fetchAndRegisterNcsOccupation(String ncsCode) {
    // 1. 직무 정보 조회
    NcsOccupationResponse response = ncsApiClient.getOccupation(ncsCode);
    List<NcsOccupationResponse.NcsOccupationItem> items = response.data();

    if (items == null || items.isEmpty()) return false;

    NcsOccupationResponse.NcsOccupationItem item = items.get(0);

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
}

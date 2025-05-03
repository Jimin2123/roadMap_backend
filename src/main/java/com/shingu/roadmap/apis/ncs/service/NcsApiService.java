package com.shingu.roadmap.apis.ncs.service;

import com.shingu.roadmap.apis.ncs.client.NcsApiClient;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationResponse;
import com.shingu.roadmap.apis.ncs.repository.NcsOccupationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NcsApiService {

  private final NcsApiClient ncsApiClient;
  private final NcsOccupationRepository ncsOccupationRepository;

  public Set<String> filterValidNcsCodes(Set<String> ncsCodes) {
    Set<String> validCodes = new HashSet<>();

    for (String code : ncsCodes) {

      // 등록된 NCS 코드인지 확인
      if(ncsOccupationRepository.existsById(code)) {
        validCodes.add(code);
        continue;
      }

      // NCS API를 통해 직무정보 조회
      NcsOccupationResponse response = ncsApiClient.getOccupation(code);
      List<NcsOccupationResponse.NcsOccupationItem> items = response.data();

      if(!items.isEmpty()) {
        NcsOccupationResponse.NcsOccupationItem item = items.get(0);

        // 유효한 응답 -> 저장
        NcsOccupation occupation = new NcsOccupation(
                item.dutyCd(),
                item.dutyNm(),
                item.dutySvcNo(),
                item.dutyDef()
        );
        ncsOccupationRepository.save(occupation);

        validCodes.add(code);
      }
    }

    return validCodes;
  }
}

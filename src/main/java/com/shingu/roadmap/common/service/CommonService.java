package com.shingu.roadmap.common.service;

import com.shingu.roadmap.common.dto.response.CertificateAutoCompleteResponse;
import com.shingu.roadmap.common.dto.response.SkillAutoCompleteResponse;
import com.shingu.roadmap.common.repository.CertificateRepository;
import com.shingu.roadmap.common.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonService {

  private final CertificateRepository certificateRepository;
  private final SkillRepository skillRepository;

  public List<CertificateAutoCompleteResponse> autoCompleteCert(String query) {
    return certificateRepository.findTop10ByJmfldnmContainingIgnoreCaseOrderByJmfldnmAsc(query)
            .stream()
            .map(CertificateAutoCompleteResponse::from)
            .toList();
  }

  public List<SkillAutoCompleteResponse> autoCompleteSkills(String query) {
    return skillRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(query)
            .stream()
            .map(SkillAutoCompleteResponse::from)
            .toList();
  }
}

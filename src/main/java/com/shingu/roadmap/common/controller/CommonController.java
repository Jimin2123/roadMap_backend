package com.shingu.roadmap.common.controller;

import com.shingu.roadmap.common.dto.response.CertificateAutoCompleteResponse;
import com.shingu.roadmap.common.dto.response.SkillAutoCompleteResponse;
import com.shingu.roadmap.common.service.CommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommonController implements CommonControllerSwagger {

  private final CommonService commonService;

  @Override
  @GetMapping("/api/v1/certs/autocomplete")
  public ResponseEntity<List<CertificateAutoCompleteResponse>> searchCerts(@RequestParam String query) {
    if (query == null || query.trim().isEmpty()) {
      return ResponseEntity.ok(Collections.emptyList());
    }

    return ResponseEntity.ok(commonService.autoCompleteCert(query.trim()));
  }

  @Override
  @GetMapping("/api/v1/skills/autocomplete")
  public ResponseEntity<List<SkillAutoCompleteResponse>> searchSkills(@RequestParam String query) {
    if (query == null || query.trim().isEmpty()) {
      return ResponseEntity.ok(Collections.emptyList());
    }

    return ResponseEntity.ok(commonService.autoCompleteSkills(query.trim()));
  }
}

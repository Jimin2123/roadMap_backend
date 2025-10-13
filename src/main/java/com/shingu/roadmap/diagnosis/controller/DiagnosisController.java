package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.diagnosis.service.DiagnosisService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DiagnosisController implements DiagnosisControllerSwagger {
  private DiagnosisService diagnosisService;

  @Override
  @PostMapping("/api/v1/diagnosis")
  public ResponseEntity<Void> runDiagnosis(
          @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    return null;
  }

  @Override
  @GetMapping("/api/v1/diagnosis/stream")
  public ResponseEntity<Void> streamDiagnosisProgress() {
    return null;
  }

  @Override
  @GetMapping("/api/v1/diagnosis/result")
  public ResponseEntity<Void> getFinalDiagnosisResult() {
    return null;
  }

  @Override
  @PostMapping("/api/v1/diagnosis/select-job")
  public ResponseEntity<Void> selectJobManually() {
    return null;
  }
}

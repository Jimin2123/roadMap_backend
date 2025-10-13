package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.diagnosis.dto.request.DiagnosisStartRequest;
import com.shingu.roadmap.diagnosis.dto.request.JobConfirmationRequest;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.service.DiagnosisService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DiagnosisController implements DiagnosisControllerSwagger {

  private final DiagnosisService diagnosisService;

  @Override
  @PostMapping("/api/v1/diagnosis")
  public ResponseEntity<DiagnosisProgressResponse> runDiagnosis(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestBody(required = false) DiagnosisStartRequest request
  ) {

    return ResponseEntity.ok(null);
  }

  @Override
  @GetMapping("/api/v1/diagnosis/{id}/stream")
  public ResponseEntity<DiagnosisProgressResponse> streamDiagnosisProgress(
          @PathVariable("id") Long diagnosisId
  ) {
    return ResponseEntity.ok(null);
  }

  @Override
  @GetMapping("/api/v1/diagnosis/result/{id}")
  public ResponseEntity<DiagnosisResultResponse> getFinalDiagnosisResult(
          @PathVariable("id") Long diagnosisId
  ) {
    return ResponseEntity.ok(null);
  }

  @Override
  @PostMapping("/api/v1/diagnosis/{id}/job-confirmation")
  public ResponseEntity<Void> selectJobManually(
          @PathVariable("id") Long diagnosisId,
          @RequestBody JobConfirmationRequest request
  ) {
    return ResponseEntity.ok().build();
  }
}

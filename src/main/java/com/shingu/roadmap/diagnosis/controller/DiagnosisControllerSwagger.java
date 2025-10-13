package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Diagnosis API", description = "진단 관련 API")
public interface DiagnosisControllerSwagger {

  @Operation(
          summary = "진단 실행",
          description = "사용자의 데이터를 기반으로 진단을 실행합니다."
  )
  ResponseEntity<Void> runDiagnosis(@AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
          summary = "진단 과정 실시간 스트리밍 (SSE)",
          description = "진단 과정의 실시간 업데이트를 스트리밍 방식으로 제공합니다."
  )
  ResponseEntity<Void> streamDiagnosisProgress();

  @Operation(
          summary = "최종 진단 결과 조회",
          description = "진단이 완료된 후 최종 결과를 조회합니다."
  )
  ResponseEntity<Void> getFinalDiagnosisResult();

  @Operation(
          summary = "(Human-in-the-loop) 사용자가 직접 직무 선택",
          description = "사용자가 직접 직무를 선택할 수 있는 기능을 제공합니다."
  )
  ResponseEntity<Void> selectJobManually();
}

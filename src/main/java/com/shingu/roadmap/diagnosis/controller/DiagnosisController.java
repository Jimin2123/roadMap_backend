package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.diagnosis.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DiagnosisController implements DiagnosisControllerSwagger {
  private DiagnosisService diagnosisService;
}

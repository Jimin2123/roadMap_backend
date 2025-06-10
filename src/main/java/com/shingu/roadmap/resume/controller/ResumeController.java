package com.shingu.roadmap.resume.controller;

import com.shingu.roadmap.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

@Controller
@RequiredArgsConstructor
public class ResumeController implements ResumeControllerSwagger {
  private final ResumeService resumeService;

  @Override
  @PostMapping("/api/v1/resume")
  public ResponseEntity<Void> createResume() {
    return null;
  }

  @Override
  @GetMapping("/api/v1/resume")
  public ResponseEntity<Void> getResume() {
    return null;
  }

  @Override
  @PutMapping("/api/v1/resume")
  public ResponseEntity<Void> updateResume() {
    return null;
  }
}

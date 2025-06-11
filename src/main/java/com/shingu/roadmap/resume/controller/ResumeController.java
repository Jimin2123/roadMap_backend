package com.shingu.roadmap.resume.controller;

import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.resume.dto.response.ResumeResponse;
import com.shingu.roadmap.resume.service.ResumeService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class ResumeController implements ResumeControllerSwagger {
  private final ResumeService resumeService;

  @Override
  @PostMapping("/api/v1/resume")
  public ResponseEntity<MemberResponse> createResume(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestBody ProfileRequest request
  ) {
    Long memberId = userDetails.getMemberId();

    MemberResponse response = resumeService.createResume(memberId, request);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping("/api/v1/resume")
  public ResponseEntity<ResumeResponse> getResume(
          @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Long memberId = userDetails.getMemberId();

    ResumeResponse response = resumeService.getResume(memberId);
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/api/v1/resume")
  public ResponseEntity<Void> updateResume() {
    return null;
  }
}

package com.shingu.roadmap.resume.service;

import com.shingu.roadmap.member.service.MemberService;
import com.shingu.roadmap.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeService {
  private final ResumeRepository resumeRepository;
  private final MemberService memberService;
}

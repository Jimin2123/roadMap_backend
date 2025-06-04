package com.shingu.roadmap.task.service;

import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.apis.saramin.service.SaraminService;
import com.shingu.roadmap.member.domain.Address;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
  private final MemberRepository memberRepository;
  private final SaraminService saraminService;

  public SaraminJobListResponse getJobListForMainPage(int page) {
    return saraminService.getJobListForMainPage(page);
  }

  public SaraminJobListResponse getJobListForMember(int start, Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Member not found"));

    Profile profile = member.getProfile();
    if (profile == null) {
      throw new EntityNotFoundException("Profile not found for member ID: " + memberId);
    }

    Address address = member.getAddress();
    if (address == null) {
      throw new EntityNotFoundException("Address not found for member ID: " + memberId);
    }

    return saraminService.getJobListForMember(start, address.getRegionCity(), profile);
  }
}

package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final OpenAiService openAiService;

    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        member.applyProfile(request);

        return MemberResponse.from(member);
    }
}

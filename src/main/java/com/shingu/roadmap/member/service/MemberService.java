package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.sun.source.doctree.SeeTree;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final OpenAiService openAiService;
    private final NcsApiService ncsApiService;

    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        member.applyProfile(request);

        if(!CollectionUtils.isEmpty(request.skills()) ||
                !CollectionUtils.isEmpty(request.certificates())) {
            Set<String> recommendedNcsCodes = openAiService.recommendNcsCodeUsingAssistant(member).block();

            if(!CollectionUtils.isEmpty(recommendedNcsCodes)) {
                Set<NcsOccupation> validCodes = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);
                member.updateNcsOccupations(validCodes);
            }
        }
        return MemberResponse.from(member);
    }
}

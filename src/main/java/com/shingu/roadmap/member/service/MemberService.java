package com.shingu.roadmap.member.service;

import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.repository.MemberRepository;
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

    @Transactional
    public MemberResponse updateProfile(Long memberId, ProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        member.applyProfile(request);

        // 무자비한 AI를 이용을 막기 위해 검토
        if(!CollectionUtils.isEmpty(request.skills()) ||
                !CollectionUtils.isEmpty(request.certificates())) {
            Set<String> ncsCodes = openAiService.recommendNcsCodes(member).block();

            // 추천된 NCS 코드가 실제 존재하는 코드인지 검증 하는 단계를 추가 해야함
            // NCS 직무정보 API를 활용해 검증할 수 있음

            member.updateNcsCodes(ncsCodes);
        }

        return MemberResponse.from(member);
    }
}

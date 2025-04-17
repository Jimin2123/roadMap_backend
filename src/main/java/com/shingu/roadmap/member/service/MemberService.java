package com.shingu.roadmap.member.service;

import com.shingu.roadmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private  final MemberRepository memberRepository;
}

package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.request.ProfileUpdateRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.service.MemberService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberControllerSwagger {
    private final MemberService memberService;

    @Override
    @PostMapping("/api/v1/member")
    public ResponseEntity<MemberResponse> signUp(
            @Valid @RequestBody MemberRequest memberRequest
    ) {
        MemberResponse response = memberService.signUp(memberRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/api/v1/member")
    public ResponseEntity<MemberResponse> getMember(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMemberId();

        MemberResponse response = memberService.getMember(memberId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/api/v1/member/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails

    ) {
        Long memberId = userDetails.getMemberId();

        ProfileResponse response =  memberService.getProfile(memberId);
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/api/v1/member/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        Long memberId = userDetails.getMemberId();

        ProfileResponse response = memberService.updateProfileOnly(memberId, request);
        return ResponseEntity.ok(response);
    }
}

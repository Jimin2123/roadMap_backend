package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberControllerSwagger {
    private final MemberService memberService;

    @Override
    @PostMapping("/api/v1/member")
    public ResponseEntity<MemberResponse> signUp(
            @RequestBody MemberRequest memberRequest
    ) {
        MemberResponse response = memberService.signUp(memberRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/api/v1/member/{id}/profile")
    public ResponseEntity<MemberResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody ProfileRequest profileRequest
    ) {
        MemberResponse response = memberService.updateProfile(id, profileRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/api/v1/member/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        MemberResponse response = memberService.getMember(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/api/v1/member/{id}/profile")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long id) {
        ProfileResponse response =  memberService.getProfile(id);
        return ResponseEntity.ok(response);
    }
}

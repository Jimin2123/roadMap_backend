package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class MemberController implements MemberControllerSwagger {
    private final MemberService memberService;

    @Override
    @PutMapping("/api/v1/member/{id}/profile")
    public ResponseEntity<MemberResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody ProfileRequest profileRequest
    ) {
        MemberResponse response = memberService.updateProfile(id, profileRequest);
        return ResponseEntity.ok(response);
    }
}

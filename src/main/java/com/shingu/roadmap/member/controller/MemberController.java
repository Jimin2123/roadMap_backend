package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;


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


    // 아직 테스트 버전입니다.
    @GetMapping("/api/v1/member/{id}")
    public Set<String> recommendCoursesForMember(@PathVariable Long id) {
        return memberService.recommendCoursesForMember(id);
    }
}

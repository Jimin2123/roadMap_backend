package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    @PutMapping("/api/v1/member/profile/{id}")
    public ResponseEntity<MemberResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody ProfileRequest profileRequest
    ) {
        MemberResponse response = memberService.updateProfile(id, profileRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/member/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        MemberResponse response = memberService.getMember(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/member/profile/{id}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long id) {
        ProfileResponse response =  memberService.getProfile(id);
        return ResponseEntity.ok(response);
    }


    // 아직 테스트 버전입니다.
    @GetMapping("/api/v1/member/{id}/courses")
    public List<TrainingCourseResponse.TrainCourseItem> getCoursesForMember(@PathVariable Long id) {
        return memberService.recommendCoursesForMember(id);
    }
}

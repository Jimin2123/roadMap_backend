package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private  final  MemberService memberService;


}

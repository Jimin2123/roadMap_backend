package com.shingu.roadmap.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import javax.validation.constraints.Pattern;

@Schema(description = "회원 등록 요청 DTO")
public record MemberRequest(
        @Schema(description = "회원 이름", example = "홍길동")
        @NotBlank
        String name,

        @Schema(description = "회원 전화번호", example = "010-1234-5678")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        String phoneNumber
) { }

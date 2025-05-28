package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Schema(description = "회원 등록 요청 DTO")
public record MemberRequest(

        @Schema(description = "로그인 정보", implementation = LoginRequest.class)
        LoginRequest loginRequest,

        @Schema(description = "회원 이름", example = "홍길동")
        @NotBlank
        String name,

        @Schema(description = "회원 생년월일", example = "1990-01-01")
        LocalDate birthDate,

        @Schema(description = "회원 전화번호", example = "010-1234-5678")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        String phoneNumber,

        @Schema(description = "회원 주소", implementation = AddressRequest.class)
        AddressRequest addressRequest
) { }

package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "회원 등록 요청 DTO")
public record MemberRequest(

        @Schema(description = "로그인 정보", implementation = LoginRequest.class)
        @NotNull(message = "로그인 정보는 필수입니다.")
        @Valid
        LoginRequest loginRequest,

        @Schema(description = "회원 이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
        String name,

        @Schema(description = "회원 생년월일", example = "1990-01-01")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @Schema(description = "회원 전화번호", example = "010-1234-5678")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-xxxx-xxxx 형식이어야 합니다.")
        String phoneNumber,

        @Schema(description = "회원 주소", implementation = AddressRequest.class)
        @Valid
        AddressRequest addressRequest
) { }

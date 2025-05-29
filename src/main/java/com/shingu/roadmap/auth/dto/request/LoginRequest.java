package com.shingu.roadmap.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 정보 요청 DTO")
public record LoginRequest(
        @Schema(description = "이메일", example = "test@example.com")
        @NotBlank
        String email,

        @Schema(description = "비밀번호", example = "test123")
        String password
) { }

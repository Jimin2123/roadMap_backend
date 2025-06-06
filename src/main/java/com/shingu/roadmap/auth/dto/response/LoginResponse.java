package com.shingu.roadmap.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 DTO")
public record LoginResponse(
    @Schema(description = "Access Token")
    String accessToken,

    @Schema(description = "Refresh Token")
    String refreshToken
) { }

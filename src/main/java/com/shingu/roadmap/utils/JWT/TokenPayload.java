package com.shingu.roadmap.utils.JWT;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT Payload")
public record TokenPayload(
        @Schema(description = "Member ID", example = "1")
        Long memberId,

        @Schema(description = "Email", example = "test@example.com")
        String email,

        @Schema(description = "Name", example = "John Doe")
        String name,

        @Schema(description = "Role", example = "USER")
        String role
) { }

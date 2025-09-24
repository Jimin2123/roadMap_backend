package com.shingu.roadmap.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "주소 요청 DTO")
public record AddressRequest(
        @Schema(description = "도로명 주소", example = "서울특별시 강남구 테헤란로 123")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 200, message = "주소는 200자를 초과할 수 없습니다.")
        String address,

        @Schema(description = "상세 주소", example = "2층 201호")
        @Size(max = 100, message = "상세주소는 100자를 초과할 수 없습니다.")
        String addressDetail,

        @Schema(description = "지번 주소", example = "서울특별시 강남구 역삼동 123-45")
        @Size(max = 200, message = "지번주소는 200자를 초과할 수 없습니다.")
        String addressJibun,

        @Schema(description = "시/군/구", example = "서울특별시 강남구")
        @Size(max = 100, message = "시/군/구는 100자를 초과할 수 없습니다.")
        String regionCity,

        @Schema(description = "우편번호", example = "06236")
        @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
        String zonecode
) { }

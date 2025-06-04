package com.shingu.roadmap.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주소 요청 DTO")
public record AddressRequest(
        @Schema(description = "주소", example = "서울특별시 강남구 역삼동 123-45")
        String address,

        @Schema(description = "상세 주소", example = "2층 201호")
        String addressDetail,

        @Schema(description = "지번 주소", example = "서울특별시 강남구 역삼동 123-45")
        String addressJibun,

        @Schema(description = "시/군/구", example = "서울특별시")
        String regionCity,

        @Schema(description = "우편번호", example = "12345")
        String zonecode
) { }

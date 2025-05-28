package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.member.domain.Address;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@Schema(description = "주소 응답 DTO")
public record AddressResponse(
        @Schema(description = "도로명 주소", example = "서울특별시 강남구 테헤란로 123")
        String address,

        @Schema(description = "지번 주소", example = "서울특별시 강남구 역삼동 456-7")
        String addressJibun,

        @Schema(description = "상세 주소", example = "2층 201호")
        String addressDetail,

        @Schema(description = "시/군/구", example = "서울특별시 강남구")
        String regionCity,

        @Schema
        String zoncode
) {
  public static AddressResponse from(Address address) {
    if (address == null) return null;

    return new AddressResponse(
            address.getAddress(),
            address.getAddressJibun(),
            address.getAddressDetail(),
            address.getRegionCity(),
            address.getZoncode()
    );
  }
}

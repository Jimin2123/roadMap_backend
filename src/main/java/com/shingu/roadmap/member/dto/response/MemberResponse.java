package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "회원 응답 DTO")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1")
        Long id,

        @Schema(description = "회원 이름", example = "홍길동")
        String name,

        @Schema(description = "생년월일", example = "1990-01-01")
        LocalDate birthDate,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "프로필 정보")
        ProfileResponse profile,

        @Schema(description = "주소 정보")
        AddressResponse address
) {
        public static MemberResponse from(Member member) {
                return new MemberResponse(
                        member.getId(),
                        member.getName(),
                        member.getBirthDate(),
                        member.getPhoneNumber(),
                        ProfileResponse.from(member.getProfile()),
                        AddressResponse.from(member.getAddress())
                );
        }
}

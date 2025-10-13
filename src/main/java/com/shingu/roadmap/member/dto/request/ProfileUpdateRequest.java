package com.shingu.roadmap.member.dto.request;

import com.shingu.roadmap.common.enums.EducationLevelType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "회원 프로필 업데이트 요청 DTO (이력서 제외)")
public record ProfileUpdateRequest(

        @Schema(description = "희망 직무 코드", example = "[84, 85]")
        @Size(max = 10, message = "희망 직무는 최대 10개까지 선택 가능합니다")
        Set<Integer> desiredJobCodes,

        @Schema(description = "현재 직업", example = "학생")
        @Size(max = 100, message = "현재 직업은 100자를 초과할 수 없습니다")
        String currentJob,

        @Schema(description = "학력", example = "ASSOCIATE_DEGREE", implementation = EducationLevelType.class)
        EducationLevelType educationLevel,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        @Size(max = 500, message = "프로필 이미지 URL은 500자를 초과할 수 없습니다")
        String profileImageUrl,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @Size(max = 30, message = "전화번호는 30자를 초과할 수 없습니다")
        String phoneNumber,

        @Schema(description = "주소 정보", implementation = AddressRequest.class)
        @Valid
        AddressRequest address,

        @ArraySchema(schema = @Schema(description = "보유 기술 목록", implementation = SkillRequest.class))
        @Size(max = 50, message = "기술은 최대 50개까지 등록 가능합니다")
        @Valid
        Set<SkillRequest> skills
) { }

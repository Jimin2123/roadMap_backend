package com.shingu.roadmap.apis.ncs.dto.response;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NCS 직무 정보 DTO")
public record NcsOccupationDto(
        @Schema(description = "NCS 직무 코드", example = "20010201")
        String code,

        @Schema(description = "직무명", example = "웹기획")
        String name,

        @Schema(description = "직무 서비스 번호", example = "123456")
        String serviceNumber,

        @Schema(description = "직무 설명", example = "웹사이트의 기획, 설계, 구축 등의 업무를 수행하는 직무")
        String description
) {
    public static NcsOccupationDto from(NcsOccupation occupation) {
        if (occupation == null) {
            return null;
        }

        return new NcsOccupationDto(
                occupation.getDutyCd(),
                occupation.getDutyNm(),
                occupation.getDutySvcNo(),
                occupation.getDutyDef()
        );
    }
}
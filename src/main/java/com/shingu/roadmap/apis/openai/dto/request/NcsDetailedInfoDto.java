package com.shingu.roadmap.apis.openai.dto.request;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.dto.response.NcsCompUnitResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * OpenAI에게 전송할 NCS 상세 정보 DTO
 * NCS 코드뿐만 아니라 직무명, 설명, 능력단위 등의 상세 정보를 포함합니다.
 */
@Schema(description = "NCS 상세 정보 DTO")
public record NcsDetailedInfoDto(
        @Schema(description = "NCS 코드", example = "20010202")
        String ncsCode,

        @Schema(description = "직무명", example = "응용SW엔지니어링")
        String dutyName,

        @Schema(description = "직무 설명")
        String dutyDescription,

        @Schema(description = "능력단위 목록")
        List<CompetencyUnitDto> competencyUnits
) {
    /**
     * 능력단위 정보 DTO
     */
    @Schema(description = "능력단위 정보")
    public record CompetencyUnitDto(
            @Schema(description = "능력단위 코드", example = "01")
            String unitCode,

            @Schema(description = "능력단위명", example = "요구사항 확인")
            String unitName,

            @Schema(description = "능력단위 설명")
            String unitDescription,

            @Schema(description = "능력단위 레벨", example = "5")
            int unitLevel
    ) {
        public static CompetencyUnitDto from(NcsCompUnitResponse.NcsCompUnitItem item) {
            return new CompetencyUnitDto(
                    item.compUnitCd(),
                    item.compUnitName(),
                    item.compUnitDef(),
                    item.compUnitLevel()
            );
        }
    }

    /**
     * NcsOccupation과 능력단위 응답으로부터 DTO 생성
     */
    public static NcsDetailedInfoDto from(NcsOccupation occupation, List<NcsCompUnitResponse.NcsCompUnitItem> compUnits) {
        List<CompetencyUnitDto> competencyUnits = compUnits != null
                ? compUnits.stream()
                    .map(CompetencyUnitDto::from)
                    .toList()
                : List.of();

        return new NcsDetailedInfoDto(
                occupation.getDutyCd(),
                occupation.getDutyNm(),
                occupation.getDutyDef(),
                competencyUnits
        );
    }
}
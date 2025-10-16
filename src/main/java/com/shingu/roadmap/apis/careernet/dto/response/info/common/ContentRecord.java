package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// --- 메인 콘텐츠 레코드 ---
@Schema(description = "직업 상세 정보")
public record ContentRecord(
        @JsonProperty("job") @Schema(description = "직업명") String job,
        @JsonProperty("ability") @Schema(description = "핵심능력") String ability,
        @JsonProperty("similarjob") @Schema(description = "유사직업명") String similarJob,
        @JsonProperty("summary") @Schema(description = "하는 일") String summary,
        @JsonProperty("aptitude") @Schema(description = "적성 및 흥미") String aptitude,

        // 중첩 객체들
        @JsonProperty("capacity_major") CapacityMajorRecord capacityMajor,
        @JsonProperty("division") DivisionRecord division,
        @JsonProperty("stateofemp") StateOfEmploymentRecord stateOfEmployment,
        @JsonProperty("prepareway") PreparationWayRecord preparationWay,
        @JsonProperty("contact") ContactRecord contact,
        @JsonProperty("job_possibility") JobPossibilityRecord jobPossibility,

        // 특성 정보
        @JsonProperty("GenCD") CharacteristicRatioRecord genderRatio,
        @JsonProperty("SchClass") CharacteristicRatioRecord schoolLevelRatio,
        @JsonProperty("lstVals") CharacteristicPreferenceRecord valuePreference,
        @JsonProperty("lstMiddleAptd") CharacteristicPreferenceRecord middleSchoolAptitude,
        @JsonProperty("lstHighAptd") CharacteristicPreferenceRecord highSchoolAptitude
) {}
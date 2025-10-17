package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// --- 메인 콘텐츠 레코드 ---
@Schema(description = "직업 상세 정보")
public record ContentRecord(
        @JsonProperty("job") @Schema(description = "직업명") String job,
        @JsonProperty("ability") @Schema(description = "핵심능력") String ability,
        @JsonProperty("similarjob") @Schema(description = "유사직업명") String similarJob,
        @JsonProperty("summary") @Schema(description = "하는 일") String summary,
        @JsonProperty("aptitude") @Schema(description = "적성 및 흥미") String aptitude,

        // 중첩 객체들
        @JsonProperty("capacity_major") List<CapacityMajorRecord> capacityMajor,
        @JsonProperty("division") List<DivisionRecord> division,
        @JsonProperty("stateofemp") List<StateOfEmploymentRecord> stateOfEmployment,
        @JsonProperty("prepareway") List<PreparationWayRecord> preparationWay,
        @JsonProperty("contact") List<ContactRecord> contact,
        @JsonProperty("job_possibility") List<JobPossibilityRecord> jobPossibility,

        // 특성 정보
        @JsonProperty("GenCD") List<CharacteristicRatioRecord> genderRatio,
        @JsonProperty("SchClass") List<CharacteristicRatioRecord> schoolLevelRatio,
        @JsonProperty("lstVals") List<CharacteristicPreferenceRecord> valuePreference,
        @JsonProperty("lstMiddleAptd") List<CharacteristicPreferenceRecord> middleSchoolAptitude,
        @JsonProperty("lstHighAptd") List<CharacteristicPreferenceRecord> highSchoolAptitude
) {}
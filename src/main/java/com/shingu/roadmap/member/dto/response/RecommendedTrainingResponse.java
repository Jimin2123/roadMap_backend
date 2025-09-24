package com.shingu.roadmap.member.dto.response;

import com.shingu.roadmap.member.domain.RecommendedTraining;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 훈련과정 응답 DTO")
public record RecommendedTrainingResponse(
        @Schema(description = "추천 훈련 ID", example = "1")
        Long id,

        @Schema(description = "훈련과정 ID", example = "TR001")
        String trainingCourseId,

        @Schema(description = "훈련과정 제목", example = "Java 웹개발 과정")
        String title,

        @Schema(description = "훈련기관 ID", example = "INST001")
        String trainingInstituteId,

        @Schema(description = "주소", example = "서울특별시 강남구")
        String address,

        @Schema(description = "정원", example = "30")
        Integer capacity
) {
    public static RecommendedTrainingResponse from(RecommendedTraining recommendedTraining) {
        if (recommendedTraining == null) return null;

        var course = recommendedTraining.getTrainingCourse();
        return new RecommendedTrainingResponse(
                recommendedTraining.getId(),
                course.getTrprId(),
                course.getTitle(),
                course.getTrainstCstId(),
                course.getAddress(),
                course.getYardMan()
        );
    }
}
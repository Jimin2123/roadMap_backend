package com.shingu.roadmap.apis.openai.dto.request;

import java.util.List;

public record TrainingRecommendationRequest(
        GptUserProfileDto userProfile,
        List<GptTrainingCourseDto> trainingCourses
) { }


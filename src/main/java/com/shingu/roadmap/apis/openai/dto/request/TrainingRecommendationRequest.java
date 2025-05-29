package com.shingu.roadmap.apis.openai.dto.request;

import com.shingu.roadmap.member.dto.response.ProfileResponse;

import java.util.List;

public record TrainingRecommendationRequest(
        ProfileResponse userProfile,
        List<GptTrainingCourseDto> trainingCourses
) { }


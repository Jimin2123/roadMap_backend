package com.shingu.roadmap.apis.openai.dto.request;

import java.util.List;

/**
 * recommendTrainingCourse 메서드에서 사용할 User Prompt 전용 DTO
 */
public record GptUserPromptRequest(
        GptUserProfileDto user,
        String address,
        List<GptTrainingCourseDto> trainings
) { }
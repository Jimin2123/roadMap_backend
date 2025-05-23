package com.shingu.roadmap.apis.openai.dto.request;

public record GptTrainingCourseDto(
        String trprId,
        String ncsCode,
        String title,
        String address
) { }

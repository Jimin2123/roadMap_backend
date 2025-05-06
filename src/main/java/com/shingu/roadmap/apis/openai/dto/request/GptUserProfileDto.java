package com.shingu.roadmap.apis.openai.dto.request;

import java.util.Map;
import java.util.Set;

public record GptUserProfileDto(
        Set<String> skills,
        Set<String> certificates,
        String desiredJob,
        String educationLevel,
        String major,
        Map<String, String> ncsCode
) { }

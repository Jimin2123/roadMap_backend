package com.shingu.roadmap.apis.openai.dto.request;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.member.domain.Certificate;
import com.shingu.roadmap.member.domain.Skill;
import com.shingu.roadmap.member.dto.response.ProfileResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record GptUserProfileDto(
        String educationLevel,
        String major,
        String desiredJob,
        List<String> certificates,
        List<String> skills,
        List<String> desiredCapabilities,
        List<String> userCapabilities
) {
  public static GptUserProfileDto from(ProfileResponse profile) {
    return new GptUserProfileDto(
            profile.educationLevel(),
            profile.major(),
            profile.desiredJob(),
            profile.certificates().stream().map(Certificate::getJmfldnm).toList(),
            profile.skills().stream().map(Skill::getName).toList(),
            profile.desiredCapabilities().stream().map(NcsOccupation::getDutyCd).toList(),
            profile.userCapabilities().stream().map(NcsOccupation::getDutyCd).toList()
    );
  }
}
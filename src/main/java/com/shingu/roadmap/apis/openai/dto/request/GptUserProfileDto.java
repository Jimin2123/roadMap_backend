package com.shingu.roadmap.apis.openai.dto.request;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobDto;
import com.shingu.roadmap.common.domain.Certificate;
import com.shingu.roadmap.member.domain.Skill;
import com.shingu.roadmap.member.dto.response.ProfileResponse;

import java.util.List;

public record GptUserProfileDto(
        String educationLevel,
        List<String> desiredJob,
        List<String> certificates,
        List<String> skills,
        List<String> desiredCapabilities,
        List<String> userCapabilities
) {
  public static GptUserProfileDto from(ProfileResponse profile) {
    return new GptUserProfileDto(
            profile.educationLevel(),
            profile.desiredJob().stream().map(SaraminJobDto::name).toList(),
            profile.certificates().stream().map(Certificate::getJmfldnm).toList(),
            profile.skills().stream().map(Skill::getName).toList(),
            profile.desiredCapabilities().stream().map(NcsOccupation::getDutyCd).toList(),
            profile.userCapabilities().stream().map(NcsOccupation::getDutyCd).toList()
    );
  }
}
package com.shingu.roadmap.apis.openai.dto.request;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationDto;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobDto;
import com.shingu.roadmap.common.dto.CertificateDTO;
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
            profile.certificates().stream().map(CertificateDTO::name).toList(),
            // 각 DTO에서 이름과 숙련도를 꺼내 "기술이름 (숙련도)" 형식의 문자열로 만듭니다.
            profile.skills().stream()
                    .map(skillDto -> String.format("%s (%s)", skillDto.name(), skillDto.proficiency()))
                    .toList(),
<<<<<<< Updated upstream

            profile.certificates().stream().map(CertificateDTO::name).toList(),
            profile.desiredCapabilities().stream().map(NcsOccupationDto::code).toList(),
            profile.userCapabilities().stream().map(NcsOccupationDto::code).toList()
=======
            profile.desiredCapabilities().stream().map(NcsOccupation::getDutyCd).toList(),
            profile.userCapabilities().stream().map(NcsOccupation::getDutyCd).toList()
>>>>>>> Stashed changes
    );
  }
}
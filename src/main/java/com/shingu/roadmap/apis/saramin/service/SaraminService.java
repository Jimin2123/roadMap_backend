package com.shingu.roadmap.apis.saramin.service;

import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.saramin.client.SaraminClient;
import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.apis.saramin.domain.SaraminJobGroup;
import com.shingu.roadmap.apis.saramin.domain.SaraminRegion;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.apis.saramin.repository.SaraminRegionRepository;
import com.shingu.roadmap.common.enums.EducationLevelType;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaraminService {

  private final SaraminRegionRepository saraminRegionRepository;
  private final SaraminClient saraminClient;
  private final OpenAiService openAiService;

  public SaraminJobListResponse getJobListForMember(int page, String regionName, Profile profile) {
    SaraminRegion region = saraminRegionRepository.findFirstByName(regionName)
        .orElseThrow(() -> new IllegalArgumentException("Region not found: " + regionName));

//    Set<String> keyword = openAiService.generateKeyword(profile).block();

    Set<Integer> jobCodes = profile.getDesiredJobs().stream().map(SaraminJob::getCode).collect(Collectors.toSet());
    List<SaraminJobGroup> groupCodeList = profile.getDesiredJobs().stream().map(SaraminJob::getGroup).toList();
    Set<Integer> groupCodes = groupCodeList.stream().map(SaraminJobGroup::getCode).collect(Collectors.toSet());

    EducationLevelType educationLevel = EducationLevelType.valueOf(profile.getEducationLevel());

    return saraminClient.getJobList(null, page, region, groupCodes, jobCodes, educationLevel);
  }

  public SaraminJobListResponse getJobListForMainPage(int page) {
    return saraminClient.getJobList(null, page, null, null, null, null);
  }
}

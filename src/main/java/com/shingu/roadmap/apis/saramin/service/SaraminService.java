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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    SaraminJobListResponse response =  saraminClient.getJobList(null, page, region, groupCodes, jobCodes, educationLevel);


    return injectLogoToJobs(response);
  }

  public SaraminJobListResponse getJobListForMainPage(int page) {
    SaraminJobListResponse response = saraminClient.getJobList(null, page, null, null, null, null);

    return injectLogoToJobs(response);
  }

  private SaraminJobListResponse injectLogoToJobs(SaraminJobListResponse res) {
    ExecutorService pool = Executors.newFixedThreadPool(21); // 동시에 21개 정도
    var futures = res.jobs().job().stream()
            .map(j -> CompletableFuture.supplyAsync(() -> injectCompanyLogo(j), pool))
            .toList();

    List<SaraminJobListResponse.Jobs.Job> updated = futures.stream().map(CompletableFuture::join).toList();
    pool.shutdown();

    return res.withJobs(updated);
  }

  private SaraminJobListResponse.Jobs.Job injectCompanyLogo(SaraminJobListResponse.Jobs.Job job) {
    var oldDetail = job.company().detail();
    String logoUrl = saraminClient.getCompanyLogo(oldDetail.href());

    var updatedDetail = new SaraminJobListResponse.Jobs.Job.Company.Detail(
            oldDetail.href(),
            oldDetail.name(),
            logoUrl
    );

    var updatedCompany = new SaraminJobListResponse.Jobs.Job.Company(updatedDetail);

    return new SaraminJobListResponse.Jobs.Job(
            job.id(),
            job.url(),
            job.active(),
            job.postingTimestamp(),
            updatedCompany,
            job.position(),
            job.keyword(),
            job.salary(),
            job.modificationTimestamp(),
            job.openingTimestamp(),
            job.expirationTimestamp(),
            job.closeType()
    );
  }
}

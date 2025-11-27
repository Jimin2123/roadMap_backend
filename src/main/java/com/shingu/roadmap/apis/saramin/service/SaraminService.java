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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaraminService implements DisposableBean {

  private final SaraminRegionRepository saraminRegionRepository;
  private final SaraminClient saraminClient;
  private final OpenAiService openAiService;

  // 로고 주입을 위한 전용 스레드 풀
  private final ExecutorService logoFetchExecutor = Executors.newFixedThreadPool(10);

  /**
   * 회원 정보 기반 채용공고 조회 (캐싱 적용)
   *
   * 캐시 키: profileId + page + regionName
   * TTL: 30분 (application.yml 설정)
   */
  @Cacheable(value = "saraminJobsForMember",
             key = "#profile.id + '_' + #page + '_' + #regionName",
             unless = "#result == null || #result.jobs() == null || #result.jobs().job() == null || #result.jobs().job().isEmpty()")
  public SaraminJobListResponse getJobListForMember(int page, String regionName, Profile profile) {
    SaraminRegion region = saraminRegionRepository.findFirstByName(regionName)
            .orElseThrow(() -> new IllegalArgumentException("Region not found: " + regionName));

    //    Set<String> keyword = openAiService.generateKeyword(profile).block();

    Set<Integer> jobCodes = profile.getDesiredJobs().stream().map(SaraminJob::getCode).collect(Collectors.toSet());
    List<SaraminJobGroup> groupCodeList = profile.getDesiredJobs().stream().map(SaraminJob::getGroup).toList();
    Set<Integer> groupCodes = groupCodeList.stream().map(SaraminJobGroup::getCode).collect(Collectors.toSet());

    EducationLevelType educationLevel = EducationLevelType.valueOf(profile.getEducationLevel());

    log.debug("Fetching Saramin job list for member profile {} from external API", profile.getId());
    SaraminJobListResponse response = saraminClient.getJobList(null, page, region, groupCodes, jobCodes, educationLevel);

    return injectLogoToJobs(response);
  }

  /**
   * 메인 페이지 채용공고 조회 (캐싱 적용)
   *
   * 캐시 키: "main" + page
   * TTL: 30분 (application.yml 설정)
   */
  @Cacheable(value = "saraminJobsMain",
             key = "'main_' + #page",
             unless = "#result == null || #result.jobs() == null || #result.jobs().job() == null || #result.jobs().job().isEmpty()")
  public SaraminJobListResponse getJobListForMainPage(int page) {
    log.debug("Fetching Saramin main page job list from external API (page: {})", page);
    SaraminJobListResponse response = saraminClient.getJobList(null, page, null, null, null, null);

    return injectLogoToJobs(response);
  }

  /**
   * 채용공고 목록에 회사 로고를 병렬로 주입
   *
   * 재사용 가능한 스레드 풀 사용 (리소스 누수 방지)
   */
  private SaraminJobListResponse injectLogoToJobs(SaraminJobListResponse res) {
    var futures = res.jobs().job().stream()
            .map(j -> CompletableFuture.supplyAsync(() -> injectCompanyLogo(j), logoFetchExecutor))
            .toList();

    List<SaraminJobListResponse.Jobs.Job> updated = futures.stream().map(CompletableFuture::join).toList();

    return res.withJobs(updated);
  }

  private SaraminJobListResponse.Jobs.Job injectCompanyLogo(SaraminJobListResponse.Jobs.Job job) {
    var oldDetail = job.company().detail();

    // href가 null이면 로고 조회를 건너뛰고 null 반환
    String logoUrl = (oldDetail.href() != null)
            ? saraminClient.getCompanyLogo(oldDetail.href())
            : null;

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

  /**
   * Bean 소멸 시 ExecutorService를 안전하게 종료합니다.
   */
  @Override
  public void destroy() throws Exception {
    log.info("Shutting down Saramin logo fetch executor...");
    logoFetchExecutor.shutdown();
    try {
      if (!logoFetchExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate in the specified time. Forcing shutdown...");
        logoFetchExecutor.shutdownNow();
        if (!logoFetchExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
          log.error("Executor did not terminate");
        }
      }
    } catch (InterruptedException e) {
      log.error("Executor shutdown interrupted", e);
      logoFetchExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    log.info("Saramin logo fetch executor shutdown completed");
  }
}

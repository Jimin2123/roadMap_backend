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
import org.springframework.data.redis.core.RedisTemplate;
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

  // [Feature 브랜치] Redis 템플릿
  private final RedisTemplate<String, Object> redisTemplate;

  // [Develop 브랜치] 로고 주입을 위한 전용 스레드 풀 (최적화됨: 매번 생성하지 않고 재사용)
  private final ExecutorService logoFetchExecutor = Executors.newFixedThreadPool(20);

  // [Feature 브랜치] 캐시 키 상수 관리
  private static final String MAIN_PAGE_CACHE_KEY = "jobs:main:page";


  /**
   * 회원 정보 기반 채용공고 조회
   * (Develop 브랜치의 @Cacheable 유지 - 사용자 맞춤 검색 캐싱)
   */
  @Cacheable(value = "saraminJobsForMember",
          key = "#profile.id + '_' + #page + '_' + #regionName",
          unless = "#result == null || #result.jobs() == null || #result.jobs().job() == null || #result.jobs().job().isEmpty()")
  public SaraminJobListResponse getJobListForMember(int page, String regionName, Profile profile) {
    SaraminRegion region = saraminRegionRepository.findFirstByName(regionName)
            .orElseThrow(() -> new IllegalArgumentException("Region not found: " + regionName));

    Set<Integer> jobCodes = profile.getDesiredJobs().stream().map(SaraminJob::getCode).collect(Collectors.toSet());
    List<SaraminJobGroup> groupCodeList = profile.getDesiredJobs().stream().map(SaraminJob::getGroup).toList();
    Set<Integer> groupCodes = groupCodeList.stream().map(SaraminJobGroup::getCode).collect(Collectors.toSet());

    EducationLevelType educationLevel = EducationLevelType.valueOf(profile.getEducationLevel());

    log.debug("Fetching Saramin job list for member profile {} from external API", profile.getId());
    SaraminJobListResponse response = saraminClient.getJobList(null, page, region, groupCodes, jobCodes, educationLevel);

    return injectLogoToJobs(response);
  }

  /**
   * 메인 페이지 채용공고 조회 (Feature 브랜치의 Redis 로직 적용)
   * - 스케줄러가 만들어둔 데이터를 우선 조회
   */
  public SaraminJobListResponse getJobListForMainPage(int page) {
    // 0~2페이지 외에는 직접 호출 (혹은 정책에 따라 변경 가능)
    // 여기서는 우리가 스케줄러로 0,1,2만 관리하므로 나머지는 직접 호출로 처리
    if (page > 2) {
      SaraminJobListResponse response = saraminClient.getJobList(null, page, null, null, null, null);
      return injectLogoToJobs(response);
    }

    String key = MAIN_PAGE_CACHE_KEY + ":" + page;

    // 1. Redis에서 조회
    Object cachedData = redisTemplate.opsForValue().get(key);

    // 2. 데이터가 있으면 바로 반환
    if (cachedData != null) {
      return (SaraminJobListResponse) cachedData;
    }

    // 3. (비상용) 데이터가 없으면 직접 갱신 후 반환
    log.warn("Cache miss for main page {}. Fetching from API...", page);
    return refreshMainPageJobsCache(page);
  }

  /**
   * [스케줄러용] 데이터 갱신 및 저장
   * - 외부 API 호출 -> 로고 주입 -> Redis 저장
   */
  public SaraminJobListResponse refreshMainPageJobsCache(int page) {
    String key = MAIN_PAGE_CACHE_KEY + ":" + page;

    log.info("Refreshing cache for main page {}", page);
    SaraminJobListResponse response = saraminClient.getJobList(null, page, null, null, null, null);

    // 로고 이미지 처리 (시간 오래 걸리는 작업)
    SaraminJobListResponse finalResponse = injectLogoToJobs(response);

    // Redis에 저장 (30분 유지)
    redisTemplate.opsForValue().set(key, finalResponse, 30, TimeUnit.MINUTES);

    return finalResponse;
  }

  /**
   * 채용공고 목록에 회사 로고를 병렬로 주입
   * (Develop 브랜치의 최적화된 ExecutorService 사용)
   */
  private SaraminJobListResponse injectLogoToJobs(SaraminJobListResponse res) {
    // null 체크 안전장치
    if (res == null || res.jobs() == null || res.jobs().job() == null) {
      return res;
    }

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

    // 생성자가 너무 길어서 기존 코드 유지 (필드 매핑 주의)
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
   * Bean 소멸 시 ExecutorService를 안전하게 종료합니다. (Develop 브랜치 기능)
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
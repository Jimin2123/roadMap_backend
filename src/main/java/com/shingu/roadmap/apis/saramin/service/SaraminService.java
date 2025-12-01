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
public class SaraminService {

  private final SaraminRegionRepository saraminRegionRepository;
  private final SaraminClient saraminClient;
  private final OpenAiService openAiService;

  // [추가] Redis 템플릿
  private final RedisTemplate<String, Object> redisTemplate;

  // [추가] 캐시 키 상수 관리
  private static final String MAIN_PAGE_CACHE_KEY = "jobs:main:page:0"; // 0페이지 기준

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
    // 0페이지가 아니면 캐싱 안 하고 직접 호출하거나, 필요시 키를 page 변수로 동적 생성
    if (page != 0) {
      // 2페이지부터는 실시간 조회 (선택사항)
      SaraminJobListResponse response = saraminClient.getJobList(null, page, null, null, null, null);
      return injectLogoToJobs(response);
    }

    // 1. Redis에서 조회
    Object cachedData = redisTemplate.opsForValue().get(MAIN_PAGE_CACHE_KEY);

    // 2. 데이터가 있으면 바로 반환
    if (cachedData != null) {
      return (SaraminJobListResponse) cachedData;
    }

    // 3. (비상용) 데이터가 없으면 직접 갱신 후 반환
    return refreshMainPageJobsCache(page);
  }

  // =================================================================
  // 2. [스케줄러용] 데이터 갱신 및 저장 (느림: 외부 연동)
  // =================================================================
  public SaraminJobListResponse refreshMainPageJobsCache(int page) {
    // API 호출
    SaraminJobListResponse response = saraminClient.getJobList(null, page, null, null, null, null);

    // 로고 이미지 처리 (시간 오래 걸리는 작업)
    SaraminJobListResponse finalResponse = injectLogoToJobs(response);

    // Redis에 저장 (30분 유지)
    redisTemplate.opsForValue().set(MAIN_PAGE_CACHE_KEY, finalResponse, 30, TimeUnit.MINUTES);

    System.out.println("✅ [Redis] 메인 페이지 데이터 갱신 완료: " + java.time.LocalDateTime.now());

    return finalResponse;
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

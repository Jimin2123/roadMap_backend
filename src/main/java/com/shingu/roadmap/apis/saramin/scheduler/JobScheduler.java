package com.shingu.roadmap.apis.saramin.scheduler;

import com.shingu.roadmap.apis.saramin.service.SaraminService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling // 스케쥴링 활성화
@RequiredArgsConstructor
public class JobScheduler implements ApplicationRunner {

    private final SaraminService saraminService;

    // 미리 캐싱할 페이지 수 (0, 1, 2 페이지 -> 총 3페이지)
    private static final int PAGES_TO_CACHE = 3;

    // 1. 서버 시작 시 자동 실행 (Warm-up)
    @Override
    public void run(ApplicationArguments args) {
        System.out.println("🚀 서버 시작: 초기 데이터 캐싱 시도 (0~" + (PAGES_TO_CACHE - 1) + " 페이지)...");

        // 0페이지부터 2페이지까지 순서대로 캐싱
        for (int page = 0; page < PAGES_TO_CACHE; page++) {
            cachePageWithRetry(page);

            // API 보호를 위해 페이지 넘어갈 때 1초 쉼
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        System.out.println("✨ 초기 데이터 적재 작업 완료!");
    }

    // 2. 30분마다 자동 실행 (Background Refresh)
    @Scheduled(fixedRate = 1000 * 60 * 30)
    public void scheduledJobRefresh() {
        System.out.println("🔄 [스케줄러] 30분 주기 데이터 갱신 시작...");

        for (int page = 0; page < PAGES_TO_CACHE; page++) {
            try {
                saraminService.refreshMainPageJobsCache(page);

                // 너무 빠른 연속 호출 방지 (1초 대기)
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("⚠️ [스케줄러] " + page + "페이지 갱신 중 에러: " + e.getMessage());
                // 여기서 에러나도 다음 페이지는 시도하도록 continue 됨
            }
        }
        System.out.println("✅ [스케줄러] 데이터 갱신 완료");
    }

    // [헬퍼 메소드] 특정 페이지를 재시도 로직과 함께 캐싱
    private void cachePageWithRetry(int page) {
        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                saraminService.refreshMainPageJobsCache(page);
                System.out.println("✅ [" + page + "페이지] 로딩 성공!");
                return; // 성공하면 종료
            } catch (Exception e) {
                System.err.println("⚠️ [" + page + "페이지] 시도 " + i + " 실패: " + e.getMessage());
                if (i < maxRetries) {
                    try {
                        Thread.sleep(2000); // 실패 시 2초 대기
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        System.err.println("❌ [" + page + "페이지] 최종 로딩 실패.");
    }
}
package com.shingu.roadmap.apis.careernet.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
@Getter
@Slf4j
public class CareerNetCodeProvider {

  private final ResourceLoader resourceLoader;

  // 파일 내용을 문자열로 저장할 변수
  private String jobInfoCodesJson;
  private String encyclopediaCodesJson;

  // 생성자를 통해 ResourceLoader 주입
  public CareerNetCodeProvider(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  // @PostConstruct: 의존성 주입이 완료된 후 초기화 작업을 위해 실행되는 메서드
  @PostConstruct
  public void loadCodes() {
    log.info("커리어넷 검색 코드 데이터를 로드합니다...");
    this.jobInfoCodesJson = readFileToString("classpath:data/jobInfomationSearchCode.json");
    this.encyclopediaCodesJson = readFileToString("classpath:data/jobEncyclopediaSearchCode.json");
    log.info("커리어넷 검색 코드 데이터 로드 완료.");
  }

  // 리소스 경로를 받아 파일 내용을 문자열로 변환하는 헬퍼 메서드
  private String readFileToString(String path) {
    try {
      Resource resource = resourceLoader.getResource(path);
      Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      log.error("{} 파일 로딩 실패", path, e);
      throw new IllegalStateException(path + " 파일을 찾을 수 없습니다.", e);
    }
  }
}
package com.shingu.roadmap.apis.saramin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory; // 이 import가 필요합니다
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SaraminApiProperties.class)
public class SaraminApiClientConfig {

  @Bean
  @Qualifier("saraminRestClient")
  public RestClient saraminRestClient(SaraminApiProperties properties) {

    // 1. 타임아웃 설정을 위한 팩토리 생성
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);  // 연결 맺는 시간: 20초
    factory.setReadTimeout(60000);    // 데이터 읽는 시간: 60초 (I/O error 해결 핵심!)

    return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(factory) // 2. 여기에 팩토리 적용
            .build();
  }
}
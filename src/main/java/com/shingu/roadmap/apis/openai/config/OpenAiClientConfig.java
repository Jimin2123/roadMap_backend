package com.shingu.roadmap.apis.openai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiClientConfig {

  @Bean
  @Qualifier("openAiWebClient")
  public WebClient openAiWebClient(OpenAiConfig config) {
    // 메모리 버퍼 크기 증가 (큰 응답 처리용)
    ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();

    return WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .exchangeStrategies(strategies)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("OpenAI-Beta", "assistants=v2")
            .build();
  }
}

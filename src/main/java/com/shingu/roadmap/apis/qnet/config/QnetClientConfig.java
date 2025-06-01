package com.shingu.roadmap.apis.qnet.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(QnetProperties.class)
public class QnetClientConfig {

  @Bean
  @Qualifier("qnetRestClient")
  public RestClient qnetRestClient(QnetProperties properties) {
    return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
  }
}

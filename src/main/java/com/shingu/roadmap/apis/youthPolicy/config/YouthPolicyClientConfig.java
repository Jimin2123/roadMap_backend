package com.shingu.roadmap.apis.youthPolicy.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(YouthPolicyProperties.class)
public class YouthPolicyClientConfig {

  @Bean
  @Qualifier("youthPolicyRestClient")
  public RestClient youthPolicyRestClient(YouthPolicyProperties properties) {
    return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
  }
}

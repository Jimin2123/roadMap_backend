package com.shingu.roadmap.apis.work24.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(Work24Properties.class)
public class Work24ClientConfig {

  @Bean
  @Qualifier("work24RestClient")
  public RestClient work24RestClient(Work24Properties properties) {
    return RestClient.builder()
            .baseUrl(properties.getTrainingCourseUrl())
            .build();
  }
}

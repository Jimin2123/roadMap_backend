package com.shingu.roadmap.apis.careernet.config;

import com.shingu.roadmap.apis.saramin.config.SaraminApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CareerNetProperties.class)
public class CareerNetClientConfig {

  @Bean
  @Qualifier("careerNetRestClient")
  public RestClient careerNetRestClient(CareerNetProperties properties) {
    return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }
}

package com.shingu.roadmap.apis.youthPolicy.config;

import com.shingu.roadmap.apis.ncs.config.NcsApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
@EnableConfigurationProperties(YouthPolicyProperties.class)
public class YouthPolicyClientConfig {
  @Bean
  @Qualifier("youthPolicyRestClient")
  public RestClient youthPolicyRestClient(YouthPolicyProperties properties) {
    return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }
}

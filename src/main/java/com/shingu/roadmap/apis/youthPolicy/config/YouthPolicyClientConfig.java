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
    try {
      // 모든 인증서를 신뢰하는 TrustManager 설정
      TrustManager[] trustAllCerts = new TrustManager[]{
              new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
              }
      };

      // SSLContext 초기화
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCerts, new SecureRandom());

      // HttpClient 생성
      HttpClient httpClient = HttpClient.newBuilder()
              .sslContext(sslContext)
              .build();

      // RestClient 생성
      return RestClient.builder()
              .requestFactory(new JdkClientHttpRequestFactory(httpClient))
              .baseUrl(properties.getBaseUrl())
              .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              // 아래 코드는 API 키 발급시 지워야 할 코드 입니다.
              .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
              .defaultHeader(HttpHeaders.COOKIE, properties.getCookie())
              .build();
    }catch(Exception e) {
      throw new IllegalStateException("SSL 설정 중 오류가 발생했습니다.", e);
    }
  }
}

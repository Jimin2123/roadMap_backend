package com.shingu.roadmap.apis.qnet.client;

import com.shingu.roadmap.apis.qnet.config.QnetProperties;
import com.shingu.roadmap.apis.qnet.dto.response.QnetExamScheduleResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@Component
public class QnetClient {
  private final QnetProperties qnetProperties;
  private final RestClient qnetRestClient;

  public QnetClient(@Qualifier("qnetRestClient") RestClient qnetRestClient, QnetProperties qnetProperties) {
    this.qnetProperties = qnetProperties;
    this.qnetRestClient = qnetRestClient;
  }

  public QnetExamScheduleResponse getExamSchedule(String qualgbCd, String jmCd) {
    String year = String.valueOf(LocalDate.now().getYear());
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(qnetProperties.getBaseUrl())
            .queryParam("serviceKey", qnetProperties.getServiceKey())
            .queryParam("numOfRows", "10")
            .queryParam("pageNo", "1")
            .queryParam("dataFormat", "JSON")
            .queryParam("implYy", year)
            .queryParam("qualgbCd", qualgbCd) // 자격구분코드
            .queryParam("jmCd", jmCd); // 자격증 코드

    URI uri = builder.build(true).encode().toUri();

    return qnetRestClient.get()
            .uri(uri)
            .retrieve()
            .body(QnetExamScheduleResponse.class);
  }
}

package com.shingu.roadmap.apis.work24.client;

import com.shingu.roadmap.apis.work24.config.Work24Properties;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class Work24Client {
  private final Work24Properties work24Properties;
  private final RestClient restClient;

  Work24Client(@Qualifier("work24RestClient") RestClient restClient, Work24Properties work24Properties) {
    this.restClient = restClient;
    this.work24Properties = work24Properties;
  }

  public TrainingCourseResponse getTrainingCourseList(String ncsCode, int pageNum) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    String endDate = LocalDate.now().plusMonths(6).format(formatter);
    String startDate = LocalDate.now().plusDays(3).format(formatter);

    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(work24Properties.getTraningCourceUrl())
            .queryParam("authKey", work24Properties.getTraningCourceKey())
            .queryParam("returnType", "JSON")
            .queryParam("outType", "1")
            .queryParam("pageNum", String.valueOf(pageNum))
            .queryParam("pageSize", "100")
            .queryParam("srchTraArea1", "41")
            .queryParam("srchNcs1", ncsCode.substring(0,2).trim())
//            .queryParam("srchNcs2", String.valueOf(Integer.parseInt(ncsCode.substring(2,4))))
//            .queryParam("srchNcs3", String.valueOf(Integer.parseInt(ncsCode.substring(4,6))))
            .queryParam("srchTraStDt", startDate)
            .queryParam("srchTraEndDt", endDate)
            .queryParam("sort", "ASC")
            .queryParam("sortCol", "TRNG_BGDE");

    String uri = builder.build(true).encode().toUriString();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(TrainingCourseResponse.class);
  }
}

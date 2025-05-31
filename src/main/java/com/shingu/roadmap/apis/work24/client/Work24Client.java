package com.shingu.roadmap.apis.work24.client;

import com.shingu.roadmap.apis.work24.config.Work24Properties;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.common.enums.Work24Region;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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

  public void getTrainingPrograms() {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    String startDate = LocalDate.now().plusDays(3).format(formatter);

    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(work24Properties.getSkillUpUrl())
            .queryParam("authKey", work24Properties.getSkillUpKey())
            .queryParam("returnType", "XML")
            .queryParam("startPage", "1")
            .queryParam("display", "100")
            .queryParam("pgmStdt", startDate)
            .queryParam("topOrgCd","15000")
            .queryParam("orgCd", "15163");

    System.out.println(builder.toUriString());

    String uri = builder.build(true).encode().toUriString();
  }


  public TrainingCourseResponse getTrainingCourseList(String ncsCode, String address, int pageNum) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    String endDate = LocalDate.now().plusMonths(6).format(formatter);
    String startDate = LocalDate.now().plusDays(3).format(formatter);

    String regionCode = Work24Region.resolveCodeByAddress(address);

    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(work24Properties.getTrainingCourseUrl())
            .queryParam("authKey", work24Properties.getTrainingCourseKey())
            .queryParam("returnType", "JSON")
            .queryParam("outType", "1")
            .queryParam("pageNum", String.valueOf(pageNum))
            .queryParam("pageSize", "100")
            .queryParam("srchTraArea1", regionCode)
            .queryParam("srchNcs1", ncsCode)
            .queryParam("srchTraStDt", startDate)
            .queryParam("srchTraEndDt", endDate)
            .queryParam("sort", "ASC")
            .queryParam("sortCol", "TRNG_BGDE");

    System.out.println(builder.toUriString());

    String uri = builder.build(true).encode().toUriString();

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(TrainingCourseResponse.class);
  }
}

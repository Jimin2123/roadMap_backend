package com.shingu.roadmap.apis.saramin.client;

import com.shingu.roadmap.apis.saramin.config.SaraminApiProperties;
import com.shingu.roadmap.apis.saramin.domain.SaraminRegion;
import com.shingu.roadmap.apis.saramin.dto.response.SaraminJobListResponse;
import com.shingu.roadmap.common.enums.EducationLevelType;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SaraminClient {
  private SaraminApiProperties properties;
  private final RestClient restClient;

  SaraminClient(@Qualifier("saraminRestClient") RestClient restClient, SaraminApiProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public SaraminJobListResponse getJobList(
          Set<String> keyword, int page, SaraminRegion region, Set<Integer> groupCodes,
          Set<Integer> jobCodes, EducationLevelType educationLevelType
          ) {

    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(properties.getBaseUrl())
            .queryParam("access-key", properties.getApiKey())
            .queryParam("count", 20)
            .queryParam("sort", "pd");

//    if(keyword != null && !keyword.isEmpty()) {
//      builder.queryParam("keywords", keyword);
//    }

    builder.queryParam("start", page); // 페이지 시작 위치 (0부터 명시)

    if(groupCodes != null && !groupCodes.isEmpty()) {
      builder.queryParam("job_mid_cd", String.join(",", groupCodes.stream().map(String::valueOf).toList()));
    }

    if(jobCodes != null && !jobCodes.isEmpty()) {
      builder.queryParam("job_cd", String.join(",", jobCodes.stream().map(String::valueOf).toList()));
    }

    if(region != null) {
      if(region.getRegionCode1() == 101000) {
        builder.queryParam("loc_cd", region.getRegionCode1());
      }else {
        builder.queryParam("loc_cd", region.getRegionCode1() + ", 101000");
      }
    }

    if(educationLevelType != null) {
      builder.queryParam("edu_lv", educationLevelType.getCode()); // 학력 조건
    }

    URI uri = builder.build(false).encode().toUri();

    System.out.println(uri);

    return restClient.get()
            .uri(uri)
            .retrieve()
            .body(SaraminJobListResponse.class);
  }

  public String getCompanyLogo(String url) {
    try {
      Connection.Response res = Jsoup.connect(url)
              .header("Range", "bytes=0-16383") // 16 KB
              .ignoreContentType(true)
              .method(Connection.Method.GET)
              .timeout(2_000)
              .followRedirects(true)
              .maxBodySize(16 * 1024)
              .execute();
      String htmlHead = res.body().split("</head>", 2)[0] + "</head>";
      return extractOgImage(htmlHead);
    } catch (Exception ex) {
      return null;
    }
  }

  /** Jsoup 대신 정규식으로 <meta property="og:image" ...> 추출 */
  private String extractOgImage(String headHtml) {
    Matcher m = OG_IMAGE_PATTERN.matcher(headHtml);
    return m.find() ? m.group(1) : null;
  }

  private static final Pattern OG_IMAGE_PATTERN = Pattern.compile(
          "<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"'][^>]*>",
          Pattern.CASE_INSENSITIVE);
}
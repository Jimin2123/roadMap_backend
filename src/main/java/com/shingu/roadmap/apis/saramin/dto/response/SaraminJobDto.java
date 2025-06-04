package com.shingu.roadmap.apis.saramin.dto.response;

import com.shingu.roadmap.apis.saramin.domain.SaraminJob;

public record SaraminJobDto(Integer code, String name, String groupName) {
  public static SaraminJobDto from(SaraminJob job) {
    return new SaraminJobDto(
            job.getCode(),
            job.getName(),
            job.getGroup() != null ? job.getGroup().getName() : null
    );
  }
}
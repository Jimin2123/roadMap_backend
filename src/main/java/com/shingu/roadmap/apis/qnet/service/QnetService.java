package com.shingu.roadmap.apis.qnet.service;

import com.shingu.roadmap.apis.qnet.client.QnetClient;
import com.shingu.roadmap.apis.qnet.dto.response.QnetExamScheduleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class QnetService {
  private  final QnetClient qnetClient;

  public List<QnetExamScheduleResponse.Item> getExamSchedule(String qualgbCd, String jmCd) {
    QnetExamScheduleResponse response = qnetClient.getExamSchedule(qualgbCd, jmCd);

      List<QnetExamScheduleResponse.Item> items = response.body().items();

      if( items == null || items.isEmpty()) { return List.of(); }

      return items;
  }
}

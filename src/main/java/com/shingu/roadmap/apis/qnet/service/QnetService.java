package com.shingu.roadmap.apis.qnet.service;

import com.shingu.roadmap.apis.qnet.client.QnetClient;
import com.shingu.roadmap.apis.qnet.dto.response.QnetExamScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QnetService {
  private  final QnetClient qnetClient;

  public QnetExamScheduleResponse getExamSchedule(String qualgbCd, String jmCd) {
    return qnetClient.getExamSchedule(qualgbCd, jmCd);
  }
}

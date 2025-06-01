package com.shingu.roadmap.apis.work24.service;

import com.shingu.roadmap.apis.work24.client.Work24Client;
import com.shingu.roadmap.apis.work24.dto.response.EmpPgmListResponse;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Work24Service {

  private final Work24Client work24Client;

  public EmpPgmListResponse getTrainingPrograms() {
    return work24Client.getTrainingPrograms(1);
  }

  public List<TrainingCourseResponse.TrainCourseItem> getAllMatchingCourses(List<String> ncsCodes, String address) {
    Set<String> seenTrprIds = new HashSet<>();
    List<TrainingCourseResponse.TrainCourseItem> result = new ArrayList<>();

    for (String ncsCode : ncsCodes) {
      int pageSize = 100;
      int pageNum = 1;

      TrainingCourseResponse firstPage = work24Client.getTrainingCourseList(ncsCode, address, pageNum);
      int totalCount = firstPage.scn_cnt();
      int totalPages = (int) Math.ceil((double) totalCount / pageSize);

      for (int i = 1; i <= totalPages; i++) {
        TrainingCourseResponse page = (i == 1) ? firstPage : work24Client.getTrainingCourseList(ncsCode, address, i);

        for (TrainingCourseResponse.TrainCourseItem item : page.srchList()) {
          if (item.ncsCd() != null && item.trprId() != null &&
                  item.ncsCd().startsWith(ncsCode) && seenTrprIds.add(item.trprId())) {
            result.add(item);
          }
        }
      }
    }

    return result;
  }
}

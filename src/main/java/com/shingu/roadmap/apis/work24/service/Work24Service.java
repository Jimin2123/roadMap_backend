package com.shingu.roadmap.apis.work24.service;

import com.shingu.roadmap.apis.work24.client.Work24Client;
import com.shingu.roadmap.apis.work24.domain.Work24EmploymentCenter;
import com.shingu.roadmap.apis.work24.dto.response.EmpPgmListResponse;
import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.member.domain.Address;
import com.shingu.roadmap.training.repository.EmploymentCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class Work24Service {

  private final Work24Client work24Client;
  private final EmploymentCenterRepository repo;

  private record EmploymentCenterIds(
          String centerCategoryId, // 개별 고용센터 category_id
          String officeCategoryId  // 관할 지방청 category_id
  ) {}

  /**
   * 주어진 도로명 주소에 해당하는 구직자취업역량 강화프로그램 목록을 1주일치 조회합니다.
   * @param address 도로명 주소 (ex: "경기 광주시 마루들길 276")
   * @return 구직자취업역량 강화프로그램 목록
   */
  public List<EmpPgmListResponse.EmpPgmSchdInvite> getTrainingPrograms(Address address) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    EmploymentCenterIds ids = resolve(address.getAddress());

    List<EmpPgmListResponse.EmpPgmSchdInvite> allPrograms = new ArrayList<>();

    for (int i = 0; i < 7; i++) {
      String startDate = LocalDate.now().plusDays(i).format(formatter);

      EmpPgmListResponse response = work24Client.getTrainingPrograms(
              1, startDate, ids.officeCategoryId(), ids.centerCategoryId());

      if (response != null && response.empPgmSchdInvite() != null) {
        List<EmpPgmListResponse.EmpPgmSchdInvite> invites = response.empPgmSchdInvite();
        allPrograms.addAll(invites);
      }
    }

    return allPrograms;
  }

  /**
   * 주어진 NCS 코드와 주소에 해당하는 모든 훈련 과정을 조회합니다.
   * @param ncsCodes NCS 코드 리스트 (ex: ["200101", "200102"])
   * @param address 도로명 주소 (ex: "경기 광주시 마루들길 276")
   * @return 훈련 과정 리스트
   */
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

  /**
   * 도로명 주소로부터 고용센터 ID·지방청 ID를 찾는다.
   * @param roadAddress 도로명 주소 (ex: "경기 광주시 마루들길 276")
   */
  private EmploymentCenterIds resolve(String roadAddress) {

    // 1) 주소 문자열 정규화
    String normAddr = normalize(roadAddress);

    // 2) 고용센터 탐색 ─ 가장 긴 문자열이 먼저 매칭되도록 정렬
    Work24EmploymentCenter center = repo.findAllCenters().stream()
            .map(c -> Map.entry(c, normalize(c.getRegionOffice())))
            .filter(e -> normAddr.contains(e.getValue()))
            .sorted(Comparator.comparingInt(
                            (Map.Entry<Work24EmploymentCenter, String> e) -> e.getValue().length())
                    .reversed())
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

    String centerId  = center != null ? center.getCategoryId() : null;
    String officeId  = deriveOfficeId(centerId);

    return new EmploymentCenterIds(centerId, officeId);
  }

  /**
   * 고용센터 ID로부터 관할 지방청 ID를 유추한다.
   * 규칙: 고용센터 ID의 앞 두 자리 + "000" (ex: "110000")
   * @param centerId 고용센터 ID (ex: "110001")
   * @return 관할 지방청 ID, 없으면 null
   */
  private String deriveOfficeId(String centerId) {
    if (centerId == null || centerId.length() < 2) return null;

    // 규칙: 앞 두 자리 + "000"
    String candidate = centerId.substring(0, 2) + "000";
    if (repo.existsByCategoryId(candidate)) return candidate;

    // 규칙 실패 시 동일 prefix 를 가진 지방청 탐색 (데이터 누락 대비)
    return repo.findAllOffices().stream()
            .map(Work24EmploymentCenter::getCategoryId)
            .filter(categoryId -> categoryId.startsWith(centerId.substring(0, 2)))
            .findFirst()
            .orElse(null);
  }

  private String normalize(String s) {
    /* 문자열 정규화 – 불필요 단어·공백 제거 */
    final Pattern ERASE = Pattern.compile(
            "\\s+|" +                       // 모든 공백
                    "(특별자치도|특별시|광역시|도|시|군|구|읍|면|동)|" +   // 행정단위
                    "(고용센터|지방고용노동청)"                         // 기관명 접미어
    );

    return s == null ? "" : ERASE.matcher(s).replaceAll("");
  }
}

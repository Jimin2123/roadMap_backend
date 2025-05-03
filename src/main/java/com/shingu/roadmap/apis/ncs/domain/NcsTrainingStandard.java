package com.shingu.roadmap.apis.ncs.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NcsTrainingStandard {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemCd; // 항목 구분 코드
  private String itemName; // 항목 구분 명
  private String itemNo; // 항목 번호
  private String defText; // 항목 세부 내용

  @OneToMany(mappedBy = "trainingStandard")
  private List<NcsOccupationStandardLink> occupations = new ArrayList<>();

  public NcsTrainingStandard(String itemCd, String itemName, String itemNo, String defText) {
    this.itemCd = itemCd;
    this.itemName = itemName;
    this.itemNo = itemNo;
    this.defText = defText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true; // 동일 참조는 무조건 true
    if (!(o instanceof NcsTrainingStandard that)) return false; // 타입이 다르면 false
    return itemCd.equals(that.itemCd) && itemNo.equals(that.itemNo); // 값 비교
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemCd, itemNo); // 동일한 두 필드 기반 해시값 생성
  }
}

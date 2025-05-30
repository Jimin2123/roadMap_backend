package com.shingu.roadmap.common.enums;

import lombok.Getter;

import javax.swing.plaf.synth.Region;
import java.util.Arrays;

public enum Work24Region {
  서울특별시("서울", "11"),
  부산광역시("부산", "26"),
  대구광역시("대구", "27"),
  인천광역시("인천", "28"),
  광주광역시("광주", "29"),
  대전광역시("대전", "30"),
  울산광역시("울산", "31"),
  세종특별자치시("세종", "36"),
  경기도("경기", "41"),
  강원도("강원", "51"),
  충청북도("충북", "43"),
  충청남도("충남", "44"),
  전라북도("전북", "45"),
  전라남도("전남", "46"),
  경상북도("경북", "47"),
  경상남도("경남", "48"),
  제주특별자치도("제주", "50");


  private final String prefix;

  @Getter
  private final String code;

  Work24Region(String prefix, String code) {
    this.prefix = prefix;
    this.code = code;
  }

  public static String resolveCodeByAddress(String fullAddress) {
    String cityPrefix = fullAddress.split(" ")[0]; // 앞 단어만 추출
    return Arrays.stream(values())
            .filter(r -> cityPrefix.startsWith(r.prefix))
            .map(Work24Region::getCode)
            .findFirst()
            .orElse("41"); // 기본: 경기
  }

}
package com.shingu.roadmap.apis.saramin.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class IndustryKeyword {

  @Id
  private Integer code; // ex) 10803

  @Column(nullable = false, length = 100)
  private String name; // ex) 호텔

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "industry_code", nullable = false)
  private Industry industry;
}
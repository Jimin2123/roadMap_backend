package com.shingu.roadmap.apis.saramin.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Industry {

  @Id
  private Integer code; // ex) 108

  @Column(nullable = false, length = 100)
  private String name; // ex) 호텔·여행·항공

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private IndustryGroup group;

  @OneToMany(mappedBy = "industry", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<IndustryKeyword> keywords = new ArrayList<>();
}
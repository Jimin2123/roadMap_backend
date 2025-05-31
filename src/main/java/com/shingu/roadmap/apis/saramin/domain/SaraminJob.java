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
public class SaraminJob {

  @Id
  private Integer code; // ex) 2259

  @Column(nullable = false, length = 100)
  private String name; // ex) 리스크 관리

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_code", nullable = false)
  private SaraminJobGroup group;

  @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<SaraminJobKeyword> keywords = new ArrayList<>();
}
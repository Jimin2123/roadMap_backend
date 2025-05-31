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
public class SaraminJobGroup {

  @Id
  private Integer code; // ex) 16

  @Column(nullable = false, length = 100)
  private String name; // ex) 기획·전략

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<SaraminJob> jobs = new ArrayList<>();
}
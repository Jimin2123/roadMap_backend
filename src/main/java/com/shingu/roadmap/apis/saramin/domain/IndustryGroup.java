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
public class IndustryGroup {
  @Id
  private Integer id; // ex) 1

  @Column(nullable = false, unique = true, length = 50)
  private String name; // ex) 서비스업

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Industry> industries = new ArrayList<>();
}

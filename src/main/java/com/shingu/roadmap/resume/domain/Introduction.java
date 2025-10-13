package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Introduction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Lob // 매우 긴 텍스트를 저장하기 위해 @Lob 어노테이션을 사용할 수 있습니다.
  @Column(name = "growth_process")
  private String growthProcess; // 성장과정

  @Lob
  @Column(name = "strengths")
  private String strengths; // 장점 및 강점

  @Lob
  @Column(name = "school_life")
  private String schoolLife; // 학교생활

  @Lob
  @Column(name = "motivation")
  private String motivation; // 지원동기
}
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

  // 아주 긴 서술을 허용하려면:
  // @Lob
  // @Column
  @Column(length = 3000)
  private String content;

  public void updateContent(String content) {
    this.content = (content == null || content.isBlank()) ? null : content.trim();
  }
}
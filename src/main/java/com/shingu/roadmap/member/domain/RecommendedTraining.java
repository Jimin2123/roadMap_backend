package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.apis.work24.domain.Work24TrainingCourse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(name = "recommended_training")
public class RecommendedTraining {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /* Member와의 연관은 Member 측 List<RecommendedTraining> 단방향 @JoinColumn 으로 유지 */

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "training_course_id",
          foreignKey = @ForeignKey(name = "fk_rectrain_course"))
  private Work24TrainingCourse trainingCourse;

  @Builder
  private RecommendedTraining(Work24TrainingCourse trainingCourse) {
    if (trainingCourse == null) throw new IllegalArgumentException("trainingCourse must not be null");
    this.trainingCourse = trainingCourse;
  }
}

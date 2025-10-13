package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "career",
        indexes = @Index(name = "idx_career_resume", columnList = "resume_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Career {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CompanyName companyName; // 회사명

    @Embedded
    private Period period; // 근무기간

    private String department; // 근무부서

    @Column(length = 1000)
    private String description; // 업무내용

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_career_resume"))
    private Resume resume;

    /* 양방향 편의 메서드 (Resume.addCareer에서 호출) */
    void setResumeInternal(Resume r) { this.resume = r; }
}

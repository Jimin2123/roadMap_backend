package com.shingu.roadmap.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class ResumeCertificateId implements Serializable {
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "certificate_jmcd", nullable = false)
    private String certificateId;
}

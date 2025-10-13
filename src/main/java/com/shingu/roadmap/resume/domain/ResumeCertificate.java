package com.shingu.roadmap.resume.domain;

import com.shingu.roadmap.common.domain.Certificate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
@Table(name = "resume_certificate")
@Builder(toBuilder = true)
public class ResumeCertificate {

    @EmbeddedId
    @Builder.Default
    private ResumeCertificateId id = new ResumeCertificateId();

    @MapsId("resumeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id",
            foreignKey = @ForeignKey(name = "fk_resumecert_resume"))
    private Resume resume;

    @MapsId("certificateId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_jmcd",
            foreignKey = @ForeignKey(name = "fk_resumecert_certificate"))
    private Certificate certificate;

    @Column(length = 8)
    private String acquiredYear;

    @Builder
    private ResumeCertificate(Resume resume, Certificate certificate, String acquiredYear) {
        if (resume == null || certificate == null) {
            throw new IllegalArgumentException("resume/certificate must not be null");
        }
        this.resume = resume;
        this.certificate = certificate;
        this.acquiredYear = normalize(acquiredYear);
    }

    public static ResumeCertificate of(Resume resume, Certificate certificate, String acquiredYear) {
        return ResumeCertificate.builder()
                .resume(resume)
                .certificate(certificate)
                .acquiredYear(acquiredYear)
                .build();
    }

    public void changeAcquiredYear(String year) {
        this.acquiredYear = normalize(year);
    }

    void setResume(Resume r) {
        this.resume = r;
    }

    void setCertificate(Certificate c) {
        this.certificate = c;
    }

    private static String normalize(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}

package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.common.domain.BaseEntity;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Member extends BaseEntity {

    private String name; // 이름
    private String phoneNumber; // 전화번호
    private LocalDate birthDate; // 생년월일

    @Embedded
    private Profile profile; // 프로필 정보

    @ElementCollection
    @CollectionTable(name = "member_skill", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>(); // 보유 기술 목록

    @ElementCollection
    @CollectionTable(name = "member_certificate", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "certificate")
    private Set<String> certificates = new HashSet<>(); // 자격증 목록

    @ManyToMany
    @JoinTable(
            name = "member_ncs",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "ncs_code")
    )
    private Set<NcsOccupation> ncsOccupations = new HashSet<>(); // NCS 직무 정보

    /**
     * 보유 기술 목록을 초기화 후 등록합니다.
     * @param skills
     */
    public void updateSkills(Set<String> skills) {
        this.skills.clear();
        if (skills != null) {
            this.skills.addAll(skills);
        }
    }

    /**
     * 자격증 목록을 초기화 후 등록합니다.
     * @param certificates
     */
    public void updateCertificates(Set<String> certificates) {
        this.certificates.clear();
        if (certificates != null) {
            this.certificates.addAll(certificates);
        }
    }

    /**
     * 회원 프로필 정보를 업데이트합니다.
     * @param request
     */
    public void applyProfile(ProfileRequest request) {
        this.profile = new Profile(
                request.educationLevel(),
                request.major(),
                request.desiredJob()
        );
        updateSkills(request.skills());
        updateCertificates(request.certificates());
    }

    public void updateNcsOccupations(Set<NcsOccupation> occupations) {
        this.ncsOccupations.clear();
        this.ncsOccupations.addAll(occupations);
    }
}

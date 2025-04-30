package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Member extends BaseEntity {
    @Column
    private String name;

    @Column
    private String phoneNumber;

    @Embedded
    private Profile profile;

    @ElementCollection
    @CollectionTable(name = "member_skill", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>(); // 보유 기술 목록

    @ElementCollection
    @CollectionTable(name = "member_certificate", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "certificate")
    private Set<String> certificates = new HashSet<>();; // 자격증 목록

    @ElementCollection
    @CollectionTable(name = "member_ncs", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "ncs_code")
    private Set<String> ncsCodes = new HashSet<>();; // 국가직무능력표준 코드 목록
}

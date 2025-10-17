package com.shingu.roadmap.diagnosis.dto.internal;

import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;

/**
 * DiagnosisService 내부에서 Member와 Profile을 함께 전달하기 위한 데이터 캐리어 레코드입니다.
 * @param member  Member 엔티티
 * @param profile 초기화된 Profile 엔티티
 */
public record MemberWithProfile(Member member, Profile profile) {
}

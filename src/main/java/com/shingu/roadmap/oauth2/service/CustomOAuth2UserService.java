package com.shingu.roadmap.oauth2.service;

import com.shingu.roadmap.auth.domain.Account;
import com.shingu.roadmap.member.domain.Email;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.oauth2.userinfo.GoogleUserInfo;
import com.shingu.roadmap.oauth2.userinfo.KakaoUserInfo;
import com.shingu.roadmap.oauth2.userinfo.NaverUserInfo;
import com.shingu.roadmap.oauth2.userinfo.OAuth2UserInfo;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = null;

        if (registrationId.equals("google")) {
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equals("naver")) {
            oAuth2UserInfo = new NaverUserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equals("kakao")) {
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();

        Member member = memberRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingMember -> {
                    org.hibernate.Hibernate.initialize(existingMember.getAccount());
                    return existingMember;
                })
                .orElseGet(() -> {
                    Account account = Account.builder()
                            .email(Email.of(email))
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .provider(provider)
                            .providerId(providerId)
                            .build();

                    Member newMember = Member.builder()
                            .name(name)
                            .role("USER")
                            .build();
                    newMember.setAccount(account);
                    return newMember;
                });

        memberRepository.save(member);

        return new CustomUserDetails(member, oAuth2User.getAttributes());
    }
}
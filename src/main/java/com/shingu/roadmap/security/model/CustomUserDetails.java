package com.shingu.roadmap.security.model;

import com.shingu.roadmap.member.domain.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {

  private final Member member;
  private Map<String, Object> attributes;

  public CustomUserDetails(Member member) {
    this.member = member;
  }

  public CustomUserDetails(Member member, Map<String, Object> attributes) {
    this.member = member;
    this.attributes = attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return member.getAccount().getEmail().getValue();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole()));
  }

  @Override
  public String getPassword() {
    return member.getAccount().getPassword();
  }

  @Override
  public String getUsername() {
    return member.getAccount().getEmail().getValue();
  }

  public Long getMemberId() {
    return member.getId();
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return UserDetails.super.isEnabled();
  }

  public Member getMember() {
    return member;
  }
}

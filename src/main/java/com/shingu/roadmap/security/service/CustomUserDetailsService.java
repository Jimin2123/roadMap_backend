package com.shingu.roadmap.security.service;

import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final MemberRepository memberRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return memberRepository.findByAccountEmail(email)
            .map(CustomUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 찾을 수 없습니다. " + email));
  }
}

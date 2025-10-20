package com.shingu.roadmap.oauth2.userinfo;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getEmail();
    String getName();
}

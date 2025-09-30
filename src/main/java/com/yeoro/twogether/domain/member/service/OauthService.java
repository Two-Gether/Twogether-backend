package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;

public interface OauthService {
    public LoginPlatform platform();
    String buildAuthorizeUrl(String redirectUri, String state);
    String exchangeCodeForAccessToken(String code, String redirectUri);
    OauthProfile getUserProfile(String accessToken);
    String encodePassword(String rawPassword);
}

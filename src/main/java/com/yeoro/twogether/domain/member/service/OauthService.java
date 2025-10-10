package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;

public interface OauthService {
    LoginPlatform platform();
    String buildAuthorizeUrl(String redirectUri, String state);
    String buildAuthorizeUrl(); // 구현체 내부에서 redirectUri/state를 처리
    String exchangeCodeForAccessToken(String code, String redirectUri);
    OauthProfile getUserProfile(String accessToken);
    String encodePassword(String rawPassword);
}

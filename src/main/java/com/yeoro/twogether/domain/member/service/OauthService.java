package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.dto.OauthProfile;

public interface OauthService {

    /**
     * 액세스 토큰을 이용해 사용자 정보를 조회
     * @param accessToken OAuth Access Token
     * @return OauthProfile (내부 공통 DTO)
     */
    OauthProfile getUserProfile(String accessToken);

    /**
     * 비밀번호 암호화
     * @param rawPassword 원본 비밀번호
     * @return 암호화된 비밀번호
     */
    String encodePassword(String rawPassword);
}

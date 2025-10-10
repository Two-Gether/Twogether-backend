package com.yeoro.twogether.domain.member.service.Impl;

import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.member.service.OauthService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.store.StateStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OauthLoginFacade {

    // 카카오 구현체 주입 (KakaoOauthService가 @Service("KAKAO") 임)
    @Qualifier("KAKAO")
    private final OauthService kakao;

    private final MemberService memberService;
    private final StateStore stateStore;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public LoginResponse handleKakaoCallback(String code, String state,
                                             HttpServletRequest request, HttpServletResponse response) {
        if (stateStore.consume(state).isEmpty()) {
            throw new ServiceException(ErrorCode.TOKEN_INVALID);
        }
        String kakaoAccessToken = kakao.exchangeCodeForAccessToken(code, redirectUri);
        return memberService.kakaoLogin(kakaoAccessToken, request, response);
    }

    public String buildKakaoAuthorizeUrl() {
        return kakao.buildAuthorizeUrl();
    }
}
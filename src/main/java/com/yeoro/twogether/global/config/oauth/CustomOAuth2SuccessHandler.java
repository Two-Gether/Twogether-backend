package com.yeoro.twogether.global.config.oauth;

import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.global.token.JwtService;
import com.yeoro.twogether.global.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 로그인 / 회원가입 후
 * 로그인 -> 메인페이지 / 회원가입 -> 연인 등록 페이지로 이동 필요
 *
 * 프론트: http://localhost:8080/oauth2/authorization/kakao 로 이동
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    @Value("${custom.site.frontUrl}")
    private String frontUrl;

    private final MemberService memberService;  // 회원 조회/저장
    private final TokenService tokenService;    // JWT Access/Refresh 발급
    private final JwtService jwtService;        // Refresh Token을 쿠키에 저장

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        // OAuth2 사용자 정보 추출
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 로그인 플랫폼 추출 (ex: kakao)
        LoginPlatform loginPlatform = LoginPlatform.from(extractProvider(authentication));

        // 소셜 플랫폼 고유 ID
        String platformId = String.valueOf(attributes.get("id"));

        // 기존 회원 처리
        if (memberService.isExistPlatformId(platformId)) {
            Long memberId = memberService.getMemberIdByPlatformId(platformId);
            String nickname = memberService.getNicknameByMemberId(memberId);
            Long partnerId = memberService.getPartnerId(memberId);
            String partnerNickname = (partnerId != null)
                    ? memberService.getNicknameByMemberId(partnerId)
                    : null;

            // Access/Refresh 토큰 발급 + 세션 저장 + 응답 전달
            tokenService.sendTokensToClient(
                    request, response,
                    tokenService.createTokenPair(memberId, nickname, partnerId, partnerNickname),
                    memberId, nickname, partnerId, partnerNickname
            );

            return;
        }

        // 신규 회원 가입
        Map<String, Object> properties = getProperties(attributes);
        String nickname = (String) properties.get("nickname");
        String profileImage = (String) properties.get("profile_image");

        log.info("신규 회원가입 요청: platformId={}, nickname={}, image={}", platformId, nickname, profileImage);

        Long memberId = memberService.signupByOauth(null, nickname, profileImage, loginPlatform, platformId);

        // 토큰 발급 + 세션 저장 + 응답 전달 (파트너 선택 필요)
        tokenService.sendTokensToClient(
                request, response,
                tokenService.createTokenPair(memberId, nickname, null, null),
                memberId, nickname, null, null
        );

    }

    // 카카오 attributes에서 nickname, profile_image 추출
    private Map<String, Object> getProperties(Map<String, Object> attributes) {
        Object propObj = attributes.get("properties");
        if (propObj instanceof Map<?, ?> map) {
            Map<String, Object> properties = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    properties.put(key, entry.getValue());
                }
            }
            return properties;
        } else {
            throw new IllegalArgumentException("attributes가 Map 타입으로 변환되지 않습니다.");
        }
    }


    // 등록된 소셜 플랫폼 ID 추출 (kakao, google 등)
    private String extractProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            log.info("registrationId: {}", registrationId);
            return registrationId;
        }
        return null;
    }
}

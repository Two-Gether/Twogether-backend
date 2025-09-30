package com.yeoro.twogether.domain.member.controller;

import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.service.OauthService;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.global.store.OtcStore;
import com.yeoro.twogether.global.store.StateStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/oauth/kakao")
public class KakaoAuthController {

    private final OauthService kakao;
    private final OtcStore otcStore;
    private final StateStore stateStore;
    private final MemberService memberService;

    @Value("${kakao.redirect-uri}") private String redirectUri;
    @Value("${custom.site.frontUrl}") private String frontUrl;

    /** 같은 탭에서 로그인 시작 (카카오 인증 페이지로 이동) */
    @GetMapping("/start")
    public void start(@RequestParam(defaultValue = "/") String returnUrl,
                      HttpServletResponse res) throws IOException {
        String state = UUID.randomUUID().toString();
        stateStore.save(state, returnUrl); // TTL 5분

        String authorize = kakao.buildAuthorizeUrl(redirectUri, state);
        res.sendRedirect(authorize);
    }

    /** 카카오 콜백: code 교환 → 프로필 조회 → 회원 조회/가입 → OTC 발급 → 프론트로 리다이렉트 */
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse res) throws IOException {
        // state 검증
        String returnUrl = stateStore.consume(state).orElse(null);
        if (returnUrl == null) {
            String err = UriComponentsBuilder.fromHttpUrl(frontUrl + "/oauth/finish")
                    .queryParam("error", "invalid_state")
                    .build(true).toUriString();
            res.sendRedirect(err);
            return;
        }

        // code → access_token
        final String accessToken;
        try {
            accessToken = kakao.exchangeCodeForAccessToken(code, redirectUri);
        } catch (Exception e) {
            String err = UriComponentsBuilder.fromHttpUrl(frontUrl + "/oauth/finish")
                    .queryParam("error", "oauth_exchange_failed")
                    .queryParam("return", returnUrl)
                    .build(true).toUriString();
            res.sendRedirect(err);
            return;
        }

        // access_token → 프로필
        final OauthProfile profile;
        try {
            profile = kakao.getUserProfile(accessToken);
        } catch (Exception e) {
            String err = UriComponentsBuilder.fromHttpUrl(frontUrl + "/oauth/finish")
                    .queryParam("error", "profile_fetch_failed")
                    .queryParam("return", returnUrl)
                    .build(true).toUriString();
            res.sendRedirect(err);
            return;
        }

        // 회원 조회/가입 (이미 회원이면 즉시 기존 ID 반환)
        final Long memberId;
        try {
            String dummyPwEncoded = kakao.encodePassword(UUID.randomUUID().toString());
            memberId = memberService.findOrCreateMember(profile, LoginPlatform.KAKAO, dummyPwEncoded);
        } catch (Exception e) {
            String err = UriComponentsBuilder.fromHttpUrl(frontUrl + "/oauth/finish")
                    .queryParam("error", "signup_or_login_failed")
                    .queryParam("return", returnUrl)
                    .build(true).toUriString();
            res.sendRedirect(err);
            return;
        }

        // OTC 발급(60초 1회) → 프론트 finish로
        String otc = otcStore.issue(memberId);
        String finish = UriComponentsBuilder.fromHttpUrl(frontUrl + "/oauth/finish")
                .queryParam("otc", otc)
                .queryParam("return", returnUrl)
                .build(true).toUriString();

        res.sendRedirect(finish);
    }
}

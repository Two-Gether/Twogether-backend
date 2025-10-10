package com.yeoro.twogether.domain.member.controller;

import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.service.Impl.OauthLoginFacade; // ← Facade 주입
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/oauth/kakao")
public class KakaoAuthController {

    private final OauthLoginFacade oauthLoginFacade;

    /** 카카오 로그인 시작: 프론트는 이 URL로 리다이렉트하면 됨 (state 생성 포함) */
    @GetMapping("/start")
    public ResponseEntity<String> start() {
        String authorizeUrl = oauthLoginFacade.buildKakaoAuthorizeUrl();
        return ResponseEntity.ok(authorizeUrl);
    }

    /** 카카오 콜백: JWT 포함 LoginResponse JSON 반환 */
    @GetMapping("/callback")
    public ResponseEntity<LoginResponse> callback(@RequestParam String code,
                                                  @RequestParam String state,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        LoginResponse body = oauthLoginFacade.handleKakaoCallback(code, state, request, response);
        return ResponseEntity.ok(body);
    }
}
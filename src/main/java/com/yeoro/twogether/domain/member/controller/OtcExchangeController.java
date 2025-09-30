package com.yeoro.twogether.domain.member.controller;

import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.global.store.OtcStore;
import com.yeoro.twogether.global.token.TokenPair;
import com.yeoro.twogether.global.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/oauth")
public class OtcExchangeController {
    private final OtcStore otcStore;
    private final MemberService memberService;
    private final TokenService tokenService;

    /**
     * OTC 교환: JWT 발급 + HttpOnly 쿠키 세팅 + LoginResponse 반환
     * - 성공: 200 OK + LoginResponse
     * - 만료/중복 사용: 410 Gone
     * - 기타 서버 오류: 500
     */
    @PostMapping("/otc/exchange")
    public ResponseEntity<LoginResponse> exchange(@RequestBody OtcStore.OtcExchangeRequest req,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {

        // 1) OTC 검증 (1회성/TTL)
        Long memberId = otcStore.consume(req.otc()).orElse(null);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.GONE).build(); // 410 Gone
        }

        try {
            // 2) 멤버 조회(응답 바디 구성용)
            Member me = memberService.getCurrentMember(memberId);
            Member partner = me.getPartner();
            Long partnerId = (partner != null ? partner.getId() : null);

            // 3) 토큰 생성 및 전송(헤더/쿠키) + Redis 저장
            TokenPair tokenPair = tokenService.createTokenPair(memberId, me.getEmail(), partnerId);
            tokenService.sendTokensToClient(httpRequest, httpResponse, tokenPair);
            tokenService.storeRefreshTokenInRedis(memberId, tokenPair.getRefreshToken());

            // 4) 응답 바디 구성
            String name = me.getName();
            String myNickname = me.getNickname(); // 파트너가 '나'에게 준 애칭
            String partnerName = (partner != null ? partner.getName() : null);
            String partnerNickname = (partner != null ? partner.getNickname() : null);
            LocalDate relationshipStartDate = me.getRelationshipStartDate();

            LoginResponse body = LoginResponse.of(
                    tokenPair.getAccessToken(),
                    memberId,
                    name,
                    myNickname,
                    partnerId,
                    partnerName,
                    partnerNickname,
                    relationshipStartDate
            );

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

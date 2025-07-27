package com.yeoro.twogether.domain.member.dto.request;

/**
 * 프론트에서 카카오 로그인 요청 시 전달
 */
public record KakaoLoginRequest(String accessToken) {
}

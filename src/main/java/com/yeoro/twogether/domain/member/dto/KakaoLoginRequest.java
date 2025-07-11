package com.yeoro.twogether.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 프론트에서 카카오 로그인 요청 시 전달받는 DTO
 */
@Getter
@Setter
public class KakaoLoginRequest {

    private String accessToken; // 프론트에서 받은 Kakao Access Token
}

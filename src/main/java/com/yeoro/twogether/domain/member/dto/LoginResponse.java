package com.yeoro.twogether.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 완료 후 클라이언트에 반환할 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;    // 액세스 토큰 (JWT)
    private Long memberId;         // 회원 ID
    private String nickname;       // 회원 닉네임
    private Long partnerId;        // 파트너 ID (nullable)
    private String partnerNickname; // 파트너 닉네임 (nullable)

    /**
     * 응답 객체 생성 헬퍼 메서드
     */
    public static LoginResponse of(String accessToken, Long memberId, String nickname, Long partnerId, String partnerNickname) {
        return new LoginResponse(accessToken, memberId, nickname, partnerId, partnerNickname);
    }
}
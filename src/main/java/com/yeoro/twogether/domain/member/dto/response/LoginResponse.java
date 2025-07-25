package com.yeoro.twogether.domain.member.dto.response;

/**
 * 로그인 완료 후 클라이언트에 반환할 응답 DTO
 */
public record LoginResponse(
        String accessToken,
        Long memberId,
        String nickname,
        Long partnerId,
        String partnerNickname
) {
    public static LoginResponse of(String accessToken, Long memberId, String nickname, Long partnerId, String partnerNickname) {
        return new LoginResponse(accessToken, memberId, nickname, partnerId, partnerNickname);
    }
}

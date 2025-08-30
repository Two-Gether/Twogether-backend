package com.yeoro.twogether.domain.member.dto.response;

import java.time.LocalDate;

/**
 * 로그인 완료 후 클라이언트에 반환할 응답 DTO
 */
public record LoginResponse(
        String accessToken,
        Long memberId,
        String nickname,
        Long partnerId,
        String partnerNickname,
        LocalDate relationshipStartDate
) {
    public static LoginResponse of(String accessToken, Long memberId, String nickname,
                                   Long partnerId, String partnerNickname,
                                   LocalDate relationshipStartDate) {
        return new LoginResponse(accessToken, memberId, nickname, partnerId, partnerNickname, relationshipStartDate);
    }
}
package com.yeoro.twogether.domain.member.dto.response;

import java.time.LocalDate;

public record LoginResponse(
        String accessToken,
        Long memberId,

        String name,              // 내 이름 (변경 가능 → 바디로 전달)
        String myNickname,        // 파트너가 '나'에게 지어준 애칭 = me.getNickname()

        Long partnerId,
        String partnerName,       // 파트너 이름
        String partnerNickname,   // 내가 파트너에게 지어준 애칭 = partner.getNickname()

        LocalDate relationshipStartDate
) {
    public static LoginResponse of(String accessToken,
                                   Long memberId,
                                   String name,
                                   String myNickname,
                                   Long partnerId,
                                   String partnerName,
                                   String partnerNickname,
                                   LocalDate relationshipStartDate) {
        return new LoginResponse(accessToken, memberId, name, myNickname,
                partnerId, partnerName, partnerNickname, relationshipStartDate);
    }
}

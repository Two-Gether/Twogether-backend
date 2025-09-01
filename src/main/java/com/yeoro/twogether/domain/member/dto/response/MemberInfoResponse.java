package com.yeoro.twogether.domain.member.dto.response;

import com.yeoro.twogether.domain.member.entity.Member;

import java.time.LocalDate;

public record MemberInfoResponse(
        Long memberId,
        String email,

        String name,            // 내 이름
        String myNickname,      // 파트너가 '나'에게 준 애칭 (me.nickname)

        String profileImageUrl,

        Long partnerId,
        String partnerName,     // 파트너 이름
        String partnerNickname, // 내가 파트너에게 준 애칭 (partner.nickname)

        LocalDate relationshipStartDate
) {
    public static MemberInfoResponse of(Member me) {
        Long partnerId = me.getPartnerId();
        Member partner = me.getPartner();

        String partnerName = (partner != null) ? partner.getName() : null;
        String partnerNickname = (partner != null) ? partner.getNickname() : null;

        return new MemberInfoResponse(
                me.getId(),
                me.getEmail(),
                me.getName(),
                me.getNickname(),           // myNickname
                me.getProfileImageUrl(),
                partnerId,
                partnerName,
                partnerNickname,
                me.getRelationshipStartDate()
        );
    }
}

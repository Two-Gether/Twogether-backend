package com.yeoro.twogether.domain.member.dto.response;

import com.yeoro.twogether.domain.member.entity.Member;

import java.time.LocalDate;

/**
 * 사용자 정보 응답 DTO
 */
public record MemberInfoResponse(
        Long memberId,
        String email,
        String nickname,
        String profileImageUrl,
        Long partnerId,
        String partnerNickname,
        LocalDate relationshipStartDate
) {
    /**
     * Member 엔티티를 응답 DTO로 변환
     */
    public static MemberInfoResponse of(Member member) {
        Long partnerId = member.getPartnerId();
        String partnerNickname = (member.getPartner() != null) ? member.getPartner().getNickname() : null;

        return new MemberInfoResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                partnerId,
                partnerNickname,
                member.getRelationshipStartDate()
        );
    }
}

package com.yeoro.twogether.domain.member.dto;

import com.yeoro.twogether.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 사용자 정보 응답 DTO
 */
@Getter
@AllArgsConstructor
public class MemberInfoResponse {
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String partnerNickname;

    /**
     * Member 엔티티를 응답 DTO로 변환하는 팩토리 메서드
     *
     * @param member Member 엔티티
     * @return MemberInfoResponse
     */
    public static MemberInfoResponse of(Member member) {
        return new MemberInfoResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getPartner() != null ? member.getPartner().getNickname() : null
        );
    }
}
package com.yeoro.twogether.domain.member.dto.response;

import com.yeoro.twogether.domain.member.entity.Gender;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;

import java.time.LocalDate;

public record MemberInfoResponse(
        Long memberId,
        String email,

        String name,            // 내 이름
        String myNickname,      // 파트너가 '나'에게 준 애칭 (me.nickname)

        String profileImageUrl, // S3 키 or presigned URL
        Gender gender,          // male/female/unknown
        String ageRange,        // 예: "20"
        String phoneNumber,
        LoginPlatform loginPlatform, // LOCAL/KAKAO 등

        Long partnerId,
        String partnerName,     // 파트너 이름
        String partnerNickname, // 내가 파트너에게 준 애칭 (partner.nickname)

        LocalDate relationshipStartDate
) {
    /** 엔티티 값을 그대로 사용 (profileImageUrl에는 S3 key가 들어감) */
    public static MemberInfoResponse of(Member me) {
        Long partnerId = me.getPartnerId();
        Member partner = me.getPartner();

        String partnerName = (partner != null) ? partner.getName() : null;
        String partnerNickname = (partner != null) ? partner.getNickname() : null;

        return new MemberInfoResponse(
                me.getId(),
                me.getEmail(),
                me.getName(),
                me.getNickname(),                  // myNickname
                me.getProfileImageUrl(),           // S3 key 그대로
                me.getGender(),
                me.getAgeRange(),
                me.getPhoneNumber(),
                me.getLoginPlatform(),
                partnerId,
                partnerName,
                partnerNickname,
                me.getRelationshipStartDate()
        );
    }

    /** presigned URL을 주입해 응답 (profileImageUrl에 presigned URL이 들어감) */
    public static MemberInfoResponse ofResolved(Member me, String resolvedProfileUrl) {
        Long partnerId = me.getPartnerId();
        Member partner = me.getPartner();

        String partnerName = (partner != null) ? partner.getName() : null;
        String partnerNickname = (partner != null) ? partner.getNickname() : null;

        return new MemberInfoResponse(
                me.getId(),
                me.getEmail(),
                me.getName(),
                me.getNickname(),                  // myNickname
                resolvedProfileUrl,                // presigned URL
                me.getGender(),
                me.getAgeRange(),
                me.getPhoneNumber(),
                me.getLoginPlatform(),
                partnerId,
                partnerName,
                partnerNickname,
                me.getRelationshipStartDate()
        );
    }
}
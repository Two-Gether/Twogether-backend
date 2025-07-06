package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;

public interface MemberService {

    boolean isExistEmail(String email);
    boolean isExistPlatformId(String platformId);
    Long getMemberId(String email);
    Long signupByOauth(String email, String nickname, String profileImage, LoginPlatform loginPlatform, String platformId);
    Long getMemberIdByPlatformId(String platformId);
    String getNicknameByMemberId(Long memberId);
    Long getPartnerId(Long memberId);
    Member getMemberByMemberId(Long memberId);
}

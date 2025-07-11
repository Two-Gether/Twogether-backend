package com.yeoro.twogether.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * OAuth 통합 사용자 정보 DTO
 * 각 플랫폼에서 받은 프로필 정보를 공통 형태로 변환하여 사용
 */
@Getter
@Setter
public class OauthProfile {

    private String email;          // 이메일 (nullable)
    private String nickname;       // 닉네임
    private String platformId;     // 플랫폼 고유 식별자
    private String profileImageUrl; // 프로필 이미지 URL (nullable)
}

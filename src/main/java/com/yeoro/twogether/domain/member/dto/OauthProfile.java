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
    private String email;
    private String phoneNumber;         // 추가
    private String name;
    private String platformId;
    private String profileImageUrl;
    private String birthday;           // 선택
    private String gender;             // 선택
    private String ageRange;           // 선택
}

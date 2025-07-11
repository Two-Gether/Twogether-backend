package com.yeoro.twogether.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 정보 수정 요청 DTO
 */
@Getter
@Setter
public class MemberUpdateRequest {
    private String nickname;
    private String password;
    private String profileImageUrl;
}
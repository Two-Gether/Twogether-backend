package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.dto.request.LoginRequest;
import com.yeoro.twogether.domain.member.dto.request.SignupRequest;
import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 사용자 서비스
 */
public interface MemberService {


    /**
     * 일반 회원가입
     */
    LoginResponse signup(SignupRequest request, HttpServletResponse response);

    /**
     * 일반 로그인
     */
    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 로컬(자체) 가입용: 이메일 존재 여부 확인 소셜(OAuth) 가입 시 사용하지 않음
     */
    boolean isExistEmail(String email);

    /**
     * 로컬 가입용: 이메일 기반 회원 ID 조회 존재하지 않으면 예외 발생
     */
    Long getMemberId(String email);

    /**
     * 소셜(OAuth) 회원가입 처리 이메일은 선택적 정보 (nullable)
     */
    Long signupByOauth(OauthProfile profile, LoginPlatform loginPlatform, String encodedPassword);

    /**
     * 플랫폼 ID 존재 여부 확인
     */
    boolean isExistPlatformId(String platformId);

    /**
     * 플랫폼 ID 기반 회원 ID 조회 존재하지 않으면 예외 발생
     */
    Long getMemberIdByPlatformId(String platformId);

    /**
     * 회원 ID 기반 닉네임 조회 존재하지 않으면 예외 발생
     */
    String getNicknameByMemberId(Long memberId);

    /**
     * 회원 ID 기반 파트너 ID 조회 파트너 없으면 null 반환
     */
    Long getPartnerId(Long memberId);

    /**
     * 파트너 연결 코드 생성
     */
    String generatePartnerCode(Long memberId);

    /**
     * 파트너 연결 처리
     */
    LoginResponse connectPartner(Long requesterId, String inputCode,
        HttpServletRequest request,
        HttpServletResponse response);

    /**
     * 플랫폼 ID 기준으로 회원 존재 시 ID 반환, 없으면 신규 가입 후 ID 반환
     */
    Long findOrCreateMember(OauthProfile profile, LoginPlatform loginPlatform, String encodedPassword);

    /**
     * 카카오 로그인 처리 OAuth 프로필 조회 → 회원 조회/가입 → JWT 발급 및 클라이언트 전달 → 응답 DTO 반환
     */
    LoginResponse kakaoLogin(String accessToken, HttpServletRequest request,
        HttpServletResponse response);

    /**
     * 로그아웃
     */
    void logout(Long memberId, String accessToken);

    /**
     * 현재 로그인한 사용자 정보 조회 @Login에서 사용자 식별 후 Member 엔티티 반환
     */
    Member getCurrentMember(Long memberId);

    /**
     * 프로필 이미지 수정
     */
    void updateProfileImage(Long memberId, String newImageUrl);

    /**
     * 닉네임 수정
     */
    void updateNickname(Long memberId, String newNickname);

    /**
     * 파트너 연결 끊기
     */
    void disconnectPartner(Long memberId);

    /**
     * 비밀번호 변경 (LOCAL 사용자 전용)
     */
    void updatePassword(Long memberId, String currentPassword, String newPassword);
}

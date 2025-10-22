package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.dto.request.LoginRequest;
import com.yeoro.twogether.domain.member.dto.request.SignupRequest;
import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.dto.response.PasswordResetVerifyResponse;
import com.yeoro.twogether.domain.member.entity.Gender;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * 사용자 서비스
 */
public interface MemberService {

    // 가입/로그인
    LoginResponse signup(SignupRequest request, HttpServletResponse response);
    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    Long findOrCreateMember(OauthProfile profile, LoginPlatform loginPlatform, String encodedPassword);

    // 조회/검증
    boolean isExistEmail(String email);
    Long getMemberId(String email);
    boolean isExistPlatformId(String platformId);
    Long getMemberIdByPlatformId(String platformId);
    String getNameByMemberId(Long memberId);
    Long getPartnerId(Long memberId);
    Member getCurrentMember(Long memberId);

    // OAuth
    Long signupByOauth(OauthProfile profile, LoginPlatform loginPlatform, String encodedPassword);
    LoginResponse kakaoLogin(String accessToken, HttpServletRequest request, HttpServletResponse response);

    // 파트너
    String generatePartnerCode(Long memberId);
    LoginResponse connectPartner(Long requesterId, String inputCode, HttpServletRequest request, HttpServletResponse response);
    void setPartnerNickname(Long requesterId, String nickname);
    void disconnectPartner(Long memberId);

    // 프로필/정보 수정
    void updateProfileImage(Long memberId, MultipartFile image);
    URL getProfileImagePresignedUrl(Long memberId);
    void updateName(Long memberId, String newName);
    void updateGender(Long memberId, Gender gender);
    void updateAgeRange(Long memberId, String ageRange);

    // 비밀번호(로그인 상태)
    void updatePassword(Long memberId, String currentPassword, String newPassword);

    // 토큰/세션
    void logout(Long memberId, String accessToken);
    LoginResponse refreshTokens(HttpServletRequest request, HttpServletResponse response);

    // 기타
    LoginResponse updateRelationshipStartDate(Long memberId, String date, HttpServletRequest request, HttpServletResponse response);
    void deleteMember(Long memberId);

    // ===== 비밀번호 찾기(비로그인) 플로우 =====
    void issuePasswordResetCode(String email);
    PasswordResetVerifyResponse verifyPasswordResetCode(String email, String code);
    void resetPasswordWithTicket(String email, String resetTicket, String newPassword,
                                 HttpServletRequest request, HttpServletResponse response);
}

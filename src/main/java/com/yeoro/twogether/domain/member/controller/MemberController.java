package com.yeoro.twogether.domain.member.controller;

import com.yeoro.twogether.domain.member.dto.request.*;
import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.dto.response.MemberInfoResponse;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.EmailVerificationService;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.global.argumentResolver.Login;
import com.yeoro.twogether.global.token.JwtService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

@Slf4j
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final EmailVerificationService emailVerificationService;
    private final MemberService memberService;
    private final JwtService jwtService;

    /**이메일 인증 번호 전송*/
    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailSendRequest request) throws MessagingException {
        emailVerificationService.sendVerificationCode(request.email());
        return ResponseEntity.ok("인증 코드가 전송되었습니다.");
    }

    /**이메일 인증 번호 확인*/
    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyCode(@RequestBody EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }


    /** 일반 회원가입 */
    @PostMapping("/signup")
    public LoginResponse signup(@RequestBody @Valid SignupRequest request,
                                HttpServletResponse response) {
        return memberService.signup(request, response);
    }


    /**  일반 로그인 */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        return memberService.login(request, httpRequest, httpResponse);
    }


    /** 카카오 OAuth 로그인 API 프론트에서 전달받은 액세스 토큰을 이용해 로그인 처리 */
    @PostMapping("/oauth/kakao")
    public LoginResponse kakaoLogin(@RequestBody KakaoLoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        return memberService.kakaoLogin(request.accessToken(), httpRequest, httpResponse);
    }


    /**파트너 코드 생성 API*/
    @PostMapping("/partner/code")
    public String generatePartnerCode(@Login Long memberId) {
        return memberService.generatePartnerCode(memberId);
    }


    /** 파트너 연결 API */
    @PostMapping("/partner/connect")
    public LoginResponse connectPartner(@Login Long requesterId,
        @RequestParam String code,
        HttpServletRequest request,
        HttpServletResponse response) {
        return memberService.connectPartner(requesterId, code, request, response);
    }


    /** 연인 애칭 지정: A가 호출하면 B의 nickname을 지정(멱등) */
    @PutMapping("/partner/nickname")
    public ResponseEntity<String> setPartnerNickname(
            @Login Long memberId,
            @RequestBody PartnerNicknameRequest request
    ) {
        memberService.setPartnerNickname(memberId, request.nickname());
        return ResponseEntity.ok( "애칭이 성공적으로 변경되었습니다.");
    }


    /** 파트너 연결 끊기: 관계 제거 + 양쪽 nickname = null */
    @DeleteMapping("/me/partner")
    public ResponseEntity<String> disconnectPartner(@Login Long memberId) {
        memberService.disconnectPartner(memberId);
        return ResponseEntity.ok("파트너 연결이 성공적으로 해제되었습니다.");
    }

    /** 로그아웃 */
    @DeleteMapping("/logout")
    public ResponseEntity<String> logout(@Login Long memberId,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String accessToken = jwtService.resolveToken(request);
        memberService.logout(memberId, accessToken);
        jwtService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    /**연애 시작 날짜 정보 추가 APi*/
    @PutMapping("/me/relationship-start-date")
    public LoginResponse setRelationshipStartDate(@Login Long memberId,
                                                  @RequestBody RelationshipStartDateRequest req,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        return memberService.updateRelationshipStartDate(memberId, req.date(), request, response);
    }

    /**사용자 정보 조회 API*/
    @GetMapping("/me")
    public MemberInfoResponse getMyInfo(@Login Long memberId) {
        Member member = memberService.getCurrentMember(memberId);
        URL presigned = memberService.getProfileImagePresignedUrl(memberId);
        return MemberInfoResponse.ofResolved(member, presigned != null ? presigned.toString() : null);
    }

    /**프로필 이미지 변경 API*/
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfileImage(
            @Login Long memberId,
            @RequestPart("image") MultipartFile image
    ) {
        memberService.updateProfileImage(memberId, image);
        return ResponseEntity.ok("프로필 이미지가 성공적으로 변경되었습니다.");
    }

    /**이름 변경 API*/
    @PutMapping("/me/name")
    public ResponseEntity<String> updateName(@Login Long memberId,
                                           @RequestBody @Valid UpdateNameRequest req) {
        memberService.updateName(memberId, req.name());
        return ResponseEntity.ok( "이름이 성공적으로 변경되었습니다.");
    }

    /** 성별 변경 */
    @PutMapping("/me/gender")
    public ResponseEntity<String> updateGender(@Login Long memberId,
                                               @RequestBody @Valid UpdateGenderRequest req) {
        memberService.updateGender(memberId, req.gender());
        return ResponseEntity.ok("성별이 성공적으로 변경되었습니다.");
    }

    /** 연령대 변경 */
    @PutMapping("/me/age-range")
    public ResponseEntity<String> updateAgeRange(@Login Long memberId,
                                                 @RequestBody @Valid UpdateAgeRangeRequest req) {
        memberService.updateAgeRange(memberId, req.ageRange());
        return ResponseEntity.ok("연령대가 성공적으로 변경되었습니다.");
    }


    /**비밀번호 변경 API (LOCAL 사용자만 가능)*/
    @PutMapping("/me/password")
    public ResponseEntity<String> updatePassword(@Login Long memberId,
                                               @RequestBody @Valid UpdatePasswordRequest req,
                                                 HttpServletResponse response) {
        memberService.updatePassword(memberId, req.currentPassword(), req.newPassword());
        jwtService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해 주세요.");
    }

    /** 내 계정 삭제 */
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMe(@Login Long memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.ok("회원탈퇴가 완료되었습니다.");
    }

    /**
     * JWT 재발급
     * 쿠키의 refreshToken을 읽어 검증하고, DB 최신 데이터로 JWT 재발급하여 응답 바디/헤더/쿠키에 내려줌
     */
    @PostMapping("/token/refresh")
    public LoginResponse refreshToken(HttpServletRequest request,
                                      HttpServletResponse response) {
        return memberService.refreshTokens(request, response);
    }
}
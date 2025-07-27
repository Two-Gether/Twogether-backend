package com.yeoro.twogether.domain.member.controller;

import com.yeoro.twogether.domain.member.dto.request.KakaoLoginRequest;
import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.dto.response.MemberInfoResponse;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.global.argumentResolver.Login;
import com.yeoro.twogether.global.token.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtService jwtService;

    /**
     * 카카오 OAuth 로그인 API 프론트에서 전달받은 액세스 토큰을 이용해 로그인 처리
     */
    @PostMapping("/oauth/kakao")
    public LoginResponse kakaoLogin(@RequestBody KakaoLoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        return memberService.kakaoLogin(request.accessToken(), httpRequest, httpResponse);
    }

    /**
     * 파트너 코드 생성 API
     */
    @PostMapping("/partner/code")
    public String generatePartnerCode(@Login Long memberId) {
        return memberService.generatePartnerCode(memberId);
    }

    /**
     * 파트너 연결 API
     */
    @PostMapping("/partner/connect")
    public LoginResponse connectPartner(@Login Long requesterId,
        @RequestParam String code,
        HttpServletRequest request,
        HttpServletResponse response) {
        return memberService.connectPartner(requesterId, code, request, response);
    }

    /**
     * 로그아웃
     */
    @DeleteMapping("/logout")
    public ResponseEntity<String> logout(@Login Long memberId, HttpServletRequest request) {
        // "Bearer ..."에서 accessToken 추출
        String accessToken = jwtService.resolveToken(request);
        memberService.logout(memberId, accessToken);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    /**
     * 사용자 정보 조회 API
     */
    @GetMapping("/me")
    public MemberInfoResponse getMyInfo(@Login Long memberId) {
        // @Login에서 사용자 정보 추출
        Member member = memberService.getCurrentMember(memberId);

        return MemberInfoResponse.of(member);
    }

    /**
     * 프로필 이미지 변경 API
     */
    @PutMapping("/me/profile-image")
    public String updateProfileImage(@Login Long memberId,
        @RequestParam String newImageUrl) {
        memberService.updateProfileImage(memberId, newImageUrl);
        return "프로필 이미지가 성공적으로 변경되었습니다.";
    }

    /**
     * 닉네임 변경 API
     */
    @PutMapping("/me/nickname")
    public String updateNickname(@Login Long memberId,
        @RequestParam String newNickname) {
        memberService.updateNickname(memberId, newNickname);
        return "닉네임이 성공적으로 변경되었습니다.";
    }

    /**
     * 파트너 연결 끊기 API
     */
    @PutMapping("/me/partner/disconnect")
    public String disconnectPartner(@Login Long memberId) {
        memberService.disconnectPartner(memberId);
        return "파트너 연결이 성공적으로 해제되었습니다.";
    }

    /**
     * 비밀번호 변경 API LOCAL 사용자만 가능
     */
    @PutMapping("/me/password")
    public String updatePassword(@Login Long memberId,
        @RequestParam String currentPassword,
        @RequestParam String newPassword) {
        memberService.updatePassword(memberId, currentPassword, newPassword);
        return "비밀번호가 성공적으로 변경되었습니다.";
    }
}
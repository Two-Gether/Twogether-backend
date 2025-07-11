package com.yeoro.twogether.domain.member.controller;

import com.yeoro.twogether.domain.member.dto.KakaoLoginRequest;
import com.yeoro.twogether.domain.member.dto.LoginResponse;
import com.yeoro.twogether.domain.member.dto.MemberInfoResponse;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 카카오 OAuth 로그인 API
     * 프론트에서 전달받은 액세스 토큰을 이용해 로그인 처리
     */
    @PostMapping("/oauth/kakao")
    public LoginResponse kakaoLogin(@RequestBody KakaoLoginRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        return memberService.kakaoLogin(request.getAccessToken(), httpRequest, httpResponse);
    }

    /**
     * 파트너 코드 생성 API
     */
    @PostMapping("/partner/code")
    public String generatePartnerCode(@RequestParam(name = "memberId") Long memberId, HttpSession session) {
        return memberService.generatePartnerCode(memberId, session);
    }

    /**
     * 파트너 연결 API
     */
    @PostMapping("/partner/connect")
    public LoginResponse connectPartner(@RequestParam Long requesterId,
                                        @RequestParam String code,
                                        HttpSession session,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        return memberService.connectPartner(requesterId, code, session, request, response);
    }

    /**
     * 사용자 정보 조회 API
     */
    @GetMapping("/me")
    public MemberInfoResponse getMyInfo(HttpServletRequest request) {
        // JWT에서 사용자 정보 추출
        Member member = memberService.getCurrentMember(request);

        return MemberInfoResponse.of(member);
    }

    /**
     * 프로필 이미지 변경 API
     */
    @PutMapping("/me/profile-image")
    public String updateProfileImage(HttpServletRequest request,
                                     @RequestParam String newImageUrl) {
        memberService.updateProfileImage(request, newImageUrl);
        return "프로필 이미지가 성공적으로 변경되었습니다.";
    }

    /**
     * 닉네임 변경 API
     */
    @PutMapping("/me/nickname")
    public String updateNickname(HttpServletRequest request,
                                 @RequestParam String newNickname) {
        memberService.updateNickname(request, newNickname);
        return "닉네임이 성공적으로 변경되었습니다.";
    }

    /**
     * 파트너 연결 끊기 API
     */
    @PutMapping("/me/partner/disconnect")
    public String disconnectPartner(HttpServletRequest request) {
        memberService.disconnectPartner(request);
        return "파트너 연결이 성공적으로 해제되었습니다.";
    }

    /**
     * 비밀번호 변경 API
     * LOCAL 사용자만 가능
     */
    @PutMapping("/me/password")
    public String updatePassword(HttpServletRequest request,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword) {
        memberService.updatePassword(request, currentPassword, newPassword);
        return "비밀번호가 성공적으로 변경되었습니다.";
    }
}
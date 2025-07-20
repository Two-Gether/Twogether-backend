package com.yeoro.twogether.domain.member.service.Impl;

import static com.yeoro.twogether.global.exception.ErrorCode.MEMBER_NOT_FOUND;

import com.yeoro.twogether.domain.member.dto.LoginResponse;
import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.repository.MemberRepository;
import com.yeoro.twogether.domain.member.service.CodeGenerator;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.member.service.OauthService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.token.JwtService;
import com.yeoro.twogether.global.token.TokenPair;
import com.yeoro.twogether.global.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final OauthService oauthService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;


    /**
     * 로컬(자체) 가입용: 이메일 존재 여부 확인 소셜(OAuth) 가입 시 사용하지 않음
     */
    @Override
    public boolean isExistEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    /**
     * 로컬 가입용: 이메일 기반 회원 ID 조회 존재하지 않으면 예외 발생
     */
    @Override
    public Long getMemberId(String email) {
        return memberRepository.findByEmail(email)
            .map(Member::getId)
            .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
    }

    /**
     * 소셜(OAuth) 회원가입 처리 이메일은 선택적 정보 (nullable)
     */
    @Override
    @Transactional
    public Long signupByOauth(String email, String nickname, String profileImage,
        LoginPlatform loginPlatform, String platformId, String encodedPassword) {
        Member newMember = Member.builder()
            .email(email)
            .nickname(nickname)
            .profileImageUrl(profileImage)
            .loginPlatform(loginPlatform)
            .platformId(platformId)
            .password(encodedPassword)
            .build();

        return memberRepository.save(newMember).getId();
    }

    /**
     * 플랫폼 ID 존재 여부 확인
     */
    @Override
    public boolean isExistPlatformId(String platformId) {
        return memberRepository.existsByPlatformId(platformId);
    }

    /**
     * 플랫폼 ID 기반 회원 ID 조회 존재하지 않으면 예외 발생
     */
    @Override
    public Long getMemberIdByPlatformId(String platformId) {
        return memberRepository.findByPlatformId(platformId)
            .map(Member::getId)
            .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
    }

    /**
     * 회원 ID 기반 닉네임 조회 존재하지 않으면 예외 발생
     */
    @Override
    public String getNicknameByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
            .map(Member::getNickname)
            .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
    }

    /**
     * 회원 ID 기반 파트너 ID 조회 파트너 없으면 null 반환
     */
    @Override
    public Long getPartnerId(Long memberId) {
        return memberRepository.findById(memberId)
            .map(Member::getPartner)
            .map(Member::getId)
            .orElse(null);
    }

    /**
     * 파트너 연결용 코드 생성 후 세션에 저장 (3분 TTL)
     */
    @Override
    @Transactional
    public String generatePartnerCode(Long memberId, HttpSession session) {
        String code = CodeGenerator.generatePartnerCode();
        session.setAttribute("PARTNER_CODE_" + code, memberId);
        session.setMaxInactiveInterval(180); // 3분
        return code;
    }

    /**
     * 입력받은 코드로 파트너 연결 연결 성공 시 JWT 갱신 및 LoginResponse 반환
     */
    @Override
    @Transactional
    public LoginResponse connectPartner(Long requesterId, String inputCode,
        HttpSession session,
        HttpServletRequest request,
        HttpServletResponse response) {
        Long partnerId = (Long) session.getAttribute("PARTNER_CODE_" + inputCode);
        if (partnerId == null) {
            throw new ServiceException(ErrorCode.PARTNER_CODE_INVALID);
        }
        if (partnerId.equals(requesterId)) {
            throw new ServiceException(ErrorCode.SELF_PARTNER_NOT_ALLOWED);
        }

        Member requester = memberRepository.findById(requesterId)
            .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
        Member partner = memberRepository.findById(partnerId)
            .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));

        requester.connectPartner(partner);
        partner.connectPartner(requester);

        memberRepository.save(requester);
        memberRepository.save(partner);

        // 모든 파트너 관련 세션 데이터 삭제
        if (session != null) {
            session.removeAttribute("PARTNER_CODE_" + inputCode);
            session.removeAttribute("partnerCode");
            session.removeAttribute("partnerCodeOwnerId");
        }

        // 파트너 연결 완료 후 JWT 갱신 및 LoginResponse 반환
        return createLoginResponse(requesterId, request, response);
    }


    /**
     * 플랫폼 ID 기준으로 회원 존재 시 ID 반환, 없으면 신규 가입 후 ID 반환
     */
    @Override
    @Transactional
    public Long findOrCreateMember(String email, String nickname, String profileImage,
        LoginPlatform loginPlatform, String platformId, String encodedPassword) {
        if (isExistPlatformId(platformId)) {
            return getMemberIdByPlatformId(platformId);
        }
        return signupByOauth(email, nickname, profileImage, loginPlatform, platformId,
            encodedPassword);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Override
    public Member getCurrentMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 프로필 이미지 수정
     */
    @Override
    @Transactional
    public void updateProfileImage(Long memberId, String newImageUrl) {
        Member member = getCurrentMember(memberId);
        member.setProfileImageUrl(newImageUrl);
        memberRepository.save(member);
    }

    /**
     * 닉네임 수정
     */
    @Override
    @Transactional
    public void updateNickname(Long memberId, String newNickname) {
        Member member = getCurrentMember(memberId);
        member.setNickname(newNickname);
        memberRepository.save(member);
    }

    /**
     * 파트너 연결 끊기
     */
    @Override
    @Transactional
    public void disconnectPartner(Long memberId) {
        Member member = getCurrentMember(memberId);
        Member partner = member.getPartner();

        if (partner != null) {
            // 서로의 partner 모두 null로 끊음
            partner.connectPartner(null);
            member.connectPartner(null);

            memberRepository.save(partner);
        }
        memberRepository.save(member);
    }

    /**
     * 비밀번호 변경 (LOCAL 사용자 전용)
     */
    @Override
    @Transactional
    public void updatePassword(Long memberId, String currentPassword,
        String newPassword) {
        Member member = getCurrentMember(memberId);

        // LOCAL 사용자만 허용
        if (member.getLoginPlatform() != LoginPlatform.LOCAL) {
            throw new ServiceException(ErrorCode.NOT_LOCAL_MEMBER);
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new ServiceException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // 새 비밀번호로 변경
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    // 로그인
    // ------------------------------------------------------------------------------------------------------------------------------

    /**
     * 카카오 로그인 처리 OAuth 프로필 조회 → 회원 조회/가입 → JWT 발급 및 클라이언트 전달 → LoginResponse 반환
     */
    @Override
    @Transactional
    public LoginResponse kakaoLogin(String accessToken, HttpServletRequest request,
        HttpServletResponse response) {
        OauthProfile profile = oauthService.getUserProfile(accessToken);

        String email = profile.getEmail();
        String nickname = profile.getNickname();
        String platformId = profile.getPlatformId();
        String profileImage = profile.getProfileImageUrl();
        String dummyPassword = oauthService.encodePassword(UUID.randomUUID().toString());

        Long memberId = findOrCreateMember(email, nickname, profileImage, LoginPlatform.KAKAO,
            platformId, dummyPassword);

        return createLoginResponse(memberId, request, response);
    }

    /**
     * 중복된 JWT 발급 및 LoginResponse 생성을 처리하는 공통 메서드
     */
    private LoginResponse createLoginResponse(Long memberId,
        HttpServletRequest request,
        HttpServletResponse response) {
        String userNickname = getNicknameByMemberId(memberId);
        Long partnerId = getPartnerId(memberId);
        String partnerNickname = partnerId != null ? getNicknameByMemberId(partnerId) : null;

        TokenPair tokenPair = tokenService.createTokenPair(memberId, userNickname, partnerId,
            partnerNickname);
        tokenService.sendTokensToClient(request, response, tokenPair, memberId, userNickname,
            partnerId, partnerNickname);

        return LoginResponse.of(tokenPair.getAccessToken(), memberId, userNickname, partnerId,
            partnerNickname);
    }
}

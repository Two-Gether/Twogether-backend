package com.yeoro.twogether.domain.member.service.Impl;

import com.yeoro.twogether.domain.member.dto.OauthProfile;
import com.yeoro.twogether.domain.member.dto.request.LoginRequest;
import com.yeoro.twogether.domain.member.dto.request.SignupRequest;
import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.entity.Gender;
import com.yeoro.twogether.domain.member.entity.LoginPlatform;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.repository.MemberRepository;
import com.yeoro.twogether.domain.member.service.EmailVerificationService;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.member.service.OauthService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.store.PartnerCodeStore;
import com.yeoro.twogether.global.token.JwtService;
import com.yeoro.twogether.global.token.TokenPair;
import com.yeoro.twogether.global.token.TokenService;
import com.yeoro.twogether.global.util.CodeGenerator;
import com.yeoro.twogether.global.util.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static com.yeoro.twogether.global.exception.ErrorCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final OauthService oauthService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PartnerCodeStore partnerCodeStore;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;

    /**
     * 일반 회원가입
     */
    @Override
    @Transactional
    public LoginResponse signup(SignupRequest request, HttpServletResponse response) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ServiceException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (!emailVerificationService.isVerified(request.email())) {
            throw new ServiceException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (!PasswordValidator.isValid(request.password())) {
            throw new ServiceException(ErrorCode.PASSWORD_NOT_VALID);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .phoneNumber(request.phoneNumber())
                .birthday(request.birthday())
                .gender(request.gender())
                .ageRange(request.ageRange())
                .loginPlatform(LoginPlatform.LOCAL)
                .build();
        memberRepository.save(member);
        emailVerificationService.clearVerificationInfo(request.email());

        String userNickname = member.getNickname();
        Long memberId = member.getId();
        Long partnerId = member.getPartnerId();
        String partnerNickname = partnerId != null ? member.getPartner().getNickname() : null;

        TokenPair tokenPair = tokenService.createTokenPair(memberId, userNickname, partnerId, partnerNickname, null);
        tokenService.sendTokensToClient(null, response, tokenPair, memberId, userNickname, partnerId, partnerNickname, null);

        return LoginResponse.of(tokenPair.getAccessToken(), memberId, userNickname, partnerId, partnerNickname, null);
    }



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
    public Long signupByOauth(OauthProfile profile, LoginPlatform loginPlatform, String encodedPassword) {
        // 이미 존재하면 해당 회원 ID 반환
        if (isExistPlatformId(profile.getPlatformId())) {
            return getMemberIdByPlatformId(profile.getPlatformId());
        }

        // 신규 회원 가입
        Member newMember = Member.builder()
                .email(profile.getEmail())
                .nickname(profile.getNickname())
                .profileImageUrl(profile.getProfileImageUrl())
                .loginPlatform(loginPlatform)
                .platformId(profile.getPlatformId())
                .password(encodedPassword)
                .phoneNumber(profile.getPhoneNumber())
                .birthday(profile.getBirthday())
                .gender(Gender.from(profile.getGender()))
                .ageRange(profile.getAgeRange())
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
    public String generatePartnerCode(Long memberId) {
        String code;
        int maxRetry = 5;

        int attempt = 0;
        do {
            if (attempt++ >= maxRetry) {
                throw new ServiceException(ErrorCode.CODE_GENERATION_FAILED);
            }
            code = CodeGenerator.generatePartnerCode(); // 이건 그대로
        } while (partnerCodeStore.exists(code)); // Redis 중복 확인

        partnerCodeStore.save(code, memberId); // TTL 3분
        return code;
    }

    /**
     * 입력받은 코드로 파트너 연결 연결 성공 시 JWT 갱신 및 LoginResponse 반환
     */
    @Override
    @Transactional
    public LoginResponse connectPartner(Long requesterId, String inputCode,
        HttpServletRequest request,
        HttpServletResponse response) {
        Long partnerId = partnerCodeStore.consume(inputCode);
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

        // 파트너 연결 완료 후 JWT 갱신 및 LoginResponse 반환
        return createLoginResponse(requesterId, request, response);
    }


    /**
     * 플랫폼 ID 기준으로 회원 존재 시 ID 반환, 없으면 신규 가입 후 ID 반환
     */
    @Override
    @Transactional
    public Long findOrCreateMember(OauthProfile profile, LoginPlatform loginPlatform, String encodedPassword) {
        if (isExistPlatformId(profile.getPlatformId())) {
            return getMemberIdByPlatformId(profile.getPlatformId());
        }
        return signupByOauth(profile, loginPlatform, encodedPassword);
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
    public LoginResponse kakaoLogin(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        OauthProfile profile = oauthService.getUserProfile(accessToken);
        String dummyPassword = oauthService.encodePassword(UUID.randomUUID().toString());

        Long memberId = signupByOauth(profile, LoginPlatform.KAKAO, dummyPassword);

        return createLoginResponse(memberId, request, response); // TokenPair 및 쿠키 포함 처리
    }

    /**
     * 중복된 JWT 발급 및 LoginResponse 생성을 처리하는 공통 메서드
     */
    private LoginResponse createLoginResponse(Long memberId,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String userNickname = getNicknameByMemberId(memberId);
        Long partnerId = getPartnerId(memberId);
        String partnerNickname = (partnerId != null) ? getNicknameByMemberId(partnerId) : null;

        LocalDate relationshipStartDate = memberRepository.findById(memberId)
                .map(Member::getRelationshipStartDate)
                .orElse(null);

        TokenPair tokenPair = tokenService.createTokenPair(
                memberId, userNickname, partnerId, partnerNickname, relationshipStartDate
        );

        tokenService.sendTokensToClient(
                request, response, tokenPair,
                memberId, userNickname, partnerId, partnerNickname, relationshipStartDate
        );

        return LoginResponse.of(
                tokenPair.getAccessToken(),
                memberId,
                userNickname,
                partnerId,
                partnerNickname,
                relationshipStartDate
        );
    }

    /**
     * 일반 로그인
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));

        // LOCAL 사용자만 가능
        if (member.getLoginPlatform() != LoginPlatform.LOCAL) {
            throw new ServiceException(ErrorCode.NOT_LOCAL_MEMBER);
        }

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new ServiceException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        return createLoginResponse(member.getId(), httpRequest, httpResponse);
    }

    /**
     * 연애 시작 날짜 추가
     */
    @Override
    @Transactional
    public LoginResponse updateRelationshipStartDate(Long memberId,
                                                     String isoDate,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        // 1) 파싱 (형식 오류 → ServiceException)
        final LocalDate date;
        try {
            date = LocalDate.parse(isoDate); // 'YYYY-MM-DD'
        } catch (DateTimeParseException e) {
            throw new ServiceException(ErrorCode.RELATIONSHIP_DATE_FORMAT_INVALID, e);
        }

        // 엔티티 변경 (도메인 규칙 위반 → IllegalArgumentException 발생)
        Member me = getCurrentMember(memberId);
        try {
            me.changeRelationshipStartDate(date); // null/미래 날짜 등 내부 검증
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ErrorCode.RELATIONSHIP_DATE_RULE_VIOLATION, ex);
        }

        Member partner = me.getPartner();
        if (partner != null) {
            try {
                partner.changeRelationshipStartDate(date);
            } catch (IllegalArgumentException ex) {
                throw new ServiceException(ErrorCode.RELATIONSHIP_DATE_RULE_VIOLATION, ex);
            }
            memberRepository.save(partner);
        }
        memberRepository.save(me);

        // 최신 데이터로 JWT 재발급
        return createLoginResponse(memberId, request, response);
    }



    /**
     * 로그아웃 처리
     * - Redis에서 Refresh Token 제거
     */
    @Override
    @Transactional
    public void logout(Long memberId, String accessToken) {
        // Refresh Token 삭제
        tokenService.removeRefreshTokenFromRedis(memberId);

        // Access Token 블랙리스트 등록
        tokenService.blacklistAccessToken(accessToken);
    }

    /**
     * JWT 재발급
     */
    @Override
    @Transactional
    public LoginResponse refreshTokens(HttpServletRequest request, HttpServletResponse response) {
        Long memberId = tokenService.getMemberIdFromRefreshToken(request);
        return createLoginResponse(memberId, request, response);
    }

}

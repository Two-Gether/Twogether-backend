package com.yeoro.twogether.domain.member.service.Impl;

import com.yeoro.twogether.domain.diary.repository.DiaryRepository;
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
import com.yeoro.twogether.domain.place.repository.PlaceRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.service.s3.HighlightS3Service;
import com.yeoro.twogether.global.service.s3.ProfileS3Service;
import com.yeoro.twogether.global.store.PartnerCodeStore;
import com.yeoro.twogether.global.token.JwtService;
import com.yeoro.twogether.global.token.TokenPair;
import com.yeoro.twogether.global.token.TokenService;
import com.yeoro.twogether.global.util.CodeGenerator;
import com.yeoro.twogether.global.util.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static com.yeoro.twogether.global.exception.ErrorCode.MEMBER_NOT_FOUND;

@Slf4j
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
    private final HighlightS3Service highlightS3Service;
    private final PlaceRepository placeRepository;
    private final DiaryRepository diaryRepository;
    private final WaypointRepository waypointRepository;
    private final WaypointItemRepository waypointItemRepository;
    private final MemberHardDeleteTx memberHardDeleteTx;
    private final ProfileS3Service profileS3Service;




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
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .gender(request.gender())
                .ageRange(request.ageRange())
                .loginPlatform(LoginPlatform.LOCAL)
                .build();

        memberRepository.save(member);
        emailVerificationService.clearVerificationInfo(request.email());

        Long memberId = member.getId();
        TokenPair tokenPair = tokenService.createTokenPair(memberId, member.getEmail(), null);
        tokenService.sendTokensToClient(null, response, tokenPair);
        tokenService.storeRefreshTokenInRedis(memberId, tokenPair.getRefreshToken());

        return LoginResponse.of(
                tokenPair.getAccessToken(),
                memberId,
                member.getName(),      // name
                member.getNickname(),  // myNickname (초기 null)
                null,                  // partnerId
                null,                  // partnerName
                null,                  // partnerNickname
                null                   // relationshipStartDate
        );
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
        if (isExistPlatformId(profile.getPlatformId())) {
            return getMemberIdByPlatformId(profile.getPlatformId());
        }
        Member newMember = Member.builder()
                .email(profile.getEmail())
                .name(profile.getName()) // KakaoProfile에서 name 매핑
                .profileImageUrl(profile.getProfileImageUrl())
                .loginPlatform(loginPlatform)
                .platformId(profile.getPlatformId())
                .password(encodedPassword)
                .phoneNumber(profile.getPhoneNumber())
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
    public String getNameByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
            .map(Member::getName)
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
        // 동일 사용자 재요청이면 기존 코드 그대로 반환
        String existing = partnerCodeStore.findCodeByMember(memberId);
        if (existing != null) return existing;

        // 동시성 제어 — 짧은 락
        boolean locked = partnerCodeStore.tryLock(memberId, 5); // 5초 락
        try {
            // 더블 체크 (락 획득 후 재확인)
            existing = partnerCodeStore.findCodeByMember(memberId);
            if (existing != null) return existing;

            String code;
            int maxRetry = 5;
            int attempt = 0;
            do {
                if (attempt++ >= maxRetry) {
                    throw new ServiceException(ErrorCode.CODE_GENERATION_FAILED);
                }
                code = CodeGenerator.generatePartnerCode();
            } while (partnerCodeStore.existsCode(code)); // 코드 자체의 충돌 방지

            // 양방향 저장(덮어쓰지 않음, TTL 동일)
            partnerCodeStore.saveBoth(code, memberId);

            // 최종 확인(경쟁 상황에서 기존값이 들어갔으면 그 값 사용)
            String finallySaved = partnerCodeStore.findCodeByMember(memberId);
            return finallySaved != null ? finallySaved : code;
        } finally {
            if (locked) partnerCodeStore.unlock(memberId);
        }
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
    @Transactional(readOnly = true)
    public Member getCurrentMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 프로필 이미지 수정
     */
    @Override
    @Transactional
    public void updateProfileImage(Long memberId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ServiceException(ErrorCode.INVALID_FILE);
        }

        Member m = getCurrentMember(memberId);
        String currentKey = m.getProfileImageUrl();

        try {
            byte[] bytes = image.getBytes();
            String newSha = sha256Hex(bytes);

            // 동일 파일이면 스킵
            boolean same = false;
            if (currentKey != null && !currentKey.isBlank()) {
                try {
                    String oldSha = profileS3Service.headSha256(currentKey);
                    same = (oldSha != null && oldSha.equalsIgnoreCase(newSha));
                } catch (Exception e) {
                    log.warn("[profile] head meta failed, proceed upload: {}", currentKey);
                }
            }
            if (same) return;

            // 새 업로드
            ProfileS3Service.UploadResult up = profileS3Service.upload(
                    memberId,
                    image.getOriginalFilename(),
                    image.getContentType(),
                    bytes
            );
            String newKey = up.key();

            // 롤백 시 새 업로드 삭제
            registerRollbackDelete(newKey);

            // 커밋 후 기존 이미지 정리
            deleteOldAfterCommit(currentKey, newKey);

            // DB 반영
            m.setProfileImageUrl(newKey);

        } catch (IOException e) {
            throw new ServiceException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // MemberServiceImpl: ProfileS3Service 사용
    @Override
    public URL getProfileImagePresignedUrl(Long memberId) {
        Member m = getCurrentMember(memberId);
        String key = m.getProfileImageUrl();
        if (key == null || key.isBlank()) return null;
        String url = profileS3Service.presignedGetUrl(key);
        try { return new URL(url); }
        catch (Exception e) { throw new ServiceException(ErrorCode.FILE_DOWNLOAD_FAILED); }
    }

    private void deleteOldAfterCommit(String oldKey, String newKey) {
        if (oldKey == null || oldKey.isBlank() || oldKey.equals(newKey)) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                profileS3Service.deleteQuietly(oldKey);
            }
        });
    }

    private void registerRollbackDelete(String key) {
        if (key == null || key.isBlank()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    profileS3Service.deleteQuietly(key);
                    log.warn("[S3] rolled back, deleted orphan profile upload: {}", key);
                }
            }
        });
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 이름 수정
     */
    @Override
    @Transactional
    public void updateName(Long memberId, String newName) {
        Member m = getCurrentMember(memberId);
        m.setName(newName);
    }

    /**
     * 파트너 별명 설정
     */
    @Override
    @Transactional
    public void setPartnerNickname(Long requesterId, String nickname) {
        // 요청자 조회
        Member me = getCurrentMember(requesterId);

        // 파트너 확인
        Member partner = me.getPartner();
        if (partner == null) {
            throw new ServiceException(ErrorCode.PARTNER_CODE_INVALID); // 파트너 미연결 상황
        }

        partner.setNickname(nickname);
        memberRepository.save(partner);
    }

    /**
     * 파트너 연결 끊기
     */
    @Override
    @Transactional
    public void disconnectPartner(Long memberId) {
        // 본인 조회
        Member me = getCurrentMember(memberId);
        Member partner = me.getPartner();

        if (partner != null) {
            // 양쪽 nickname 초기화
            partner.setNickname(null);
            me.setNickname(null);

            // 양쪽 연애 날짜 초기화
            partner.clearRelationshipStartDate();
            me.clearRelationshipStartDate();

            // 파트너 끊기 (상호)
            partner.connectPartner(null);
            me.connectPartner(null);

            memberRepository.save(partner);
        }
        memberRepository.save(me);
    }

    /**
     * 비밀번호 변경 (LOCAL 사용자 전용)
     */
    @Override
    @Transactional
    public void updatePassword(Long memberId, String currentPassword, String newPassword) {
        Member m = getCurrentMember(memberId);

        if (m.getLoginPlatform() != LoginPlatform.LOCAL) {
            throw new ServiceException(ErrorCode.NOT_LOCAL_MEMBER);
        }
        String encoded = m.getPassword();
        if (encoded == null) {
            throw new ServiceException(ErrorCode.PASSWORD_NOT_SET);
        }
        if (!passwordEncoder.matches(currentPassword, encoded)) {
            throw new ServiceException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        if (passwordEncoder.matches(newPassword, encoded)) {
            throw new ServiceException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
        m.setPassword(passwordEncoder.encode(newPassword));

        // Refresh Token 무효화
        jwtService.invalidateRefreshToken(memberId);
        String currentAccessToken = jwtService.resolveAccessTokenFromContextOrRequest();
        if (currentAccessToken != null) {
            jwtService.blacklistAccessToken(currentAccessToken);
        }
    }

    /** 성별 변경 */
    @Override
    @Transactional
    public void updateGender(Long memberId, Gender gender) {
        Member m = getCurrentMember(memberId);
        m.setGender( gender );
    }

    /** 연령대 변경 */
    @Override
    @Transactional
    public void updateAgeRange(Long memberId, String ageRange) {
        Member m = getCurrentMember(memberId);
        m.setAgeRange( ageRange );
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
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));

        Member partner = me.getPartner();
        Long partnerId = (partner != null) ? partner.getId() : null;

        // 토큰
        TokenPair tokenPair = tokenService.createTokenPair(
                me.getId(),
                me.getEmail(),
                partnerId
        );

        // 전송
        tokenService.sendTokensToClient(request, response, tokenPair);
        tokenService.storeRefreshTokenInRedis(memberId, tokenPair.getRefreshToken());

        //
        String name = me.getName();
        String myNickname = me.getNickname(); // 파트너가 '나'에게 준 애칭
        String partnerName = (partner != null) ? partner.getName() : null;
        String partnerNickname = (partner != null) ? partner.getNickname() : null;

        LocalDate relationshipStartDate = me.getRelationshipStartDate();

        return LoginResponse.of(
                tokenPair.getAccessToken(),
                memberId,
                name,
                myNickname,
                partnerId,
                partnerName,
                partnerNickname,
                relationshipStartDate
        );
    }


    public LoginResponse privateCreateLoginResponse(Long memberId,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        // 내부 공통 로직을 그대로 재사용
        return createLoginResponse(memberId, request, response);
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

    /**
     * 회원 탈퇴
     */
    @Override
    @Transactional
    public void deleteMember(Long memberId) {
        Member me = memberRepository.findByIdWithPartner(memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> placeIds    = placeRepository.findIdsByMemberId(memberId);
        List<Long> diaryIds    = diaryRepository.findIdsByMemberId(memberId);
        List<Long> waypointIds = waypointRepository.findIdsByMemberId(memberId);

        // S3 먼저 확인
        if (!placeIds.isEmpty()) {
            placeRepository.findAllById(placeIds).forEach(p -> {
                String key = extractKey(p.getImageUrl());
                if (key != null && !key.isBlank()) highlightS3Service.delete(key);
            });
        }
        if (!waypointIds.isEmpty()) {
            waypointItemRepository.findAllByWaypoint_IdIn(waypointIds).forEach(item -> {
                String key = extractKey(item.getImageUrl());
                if (key != null && !key.isBlank()) highlightS3Service.delete(key);
            });
        }

        // DB 삭제 (동일 트랜잭션 안으로 들어감)
        memberHardDeleteTx.run(me, placeIds, diaryIds, waypointIds);
    }


    /**
     * imageUrl 컬럼에 "Key"만 저장되면 그대로 반환.
     * 혹시 전체 URL이 저장되어 있다면 path에서 Key를 추출.
     */
    private static String extractKey(String imageUrlOrKey) {
        // Key 형태면 그대로
        if (!imageUrlOrKey.startsWith("http://") && !imageUrlOrKey.startsWith("https://")) {
            return imageUrlOrKey;
        }
        try {
            URI uri = URI.create(imageUrlOrKey);
            String path = uri.getPath(); // "/folder/file.png"
            return (path != null && path.startsWith("/")) ? path.substring(1) : path;
        } catch (Exception e) {
            // URL 파싱 실패 → 원문 사용(레거시 대비)
            return imageUrlOrKey;
        }
    }
}


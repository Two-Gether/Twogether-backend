package com.yeoro.twogether.global.token;

import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.access_expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh_expiration}")
    private Long refreshExpiration;

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Access/Refresh 토큰 쌍 생성
     * - sub/jti는 JwtService가 자동 세팅 (memberId 기준)
     * - 기존 호환을 위해 커스텀 클레임(memberId, email, partnerId)도 그대로 포함
     */
    public TokenPair createTokenPair(Long memberId, String email, Long partnerId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("email", email);
        claims.put("partnerId", partnerId);

        String accessToken  = jwtService.createToken(claims, accessExpiration,  memberId);
        String refreshToken = jwtService.createToken(claims, refreshExpiration, memberId);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 클라이언트에 토큰 전달 (헤더 + 쿠키)
     * - Access: Authorization 헤더(Bearer)
     * - Refresh: HttpOnly 쿠키
     */
    public void sendTokensToClient(HttpServletRequest request,
                                   HttpServletResponse response,
                                   TokenPair tokenPair) {
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.getAccessToken());
        jwtService.setRefreshTokenCookie(tokenPair.getRefreshToken(), response);
    }

    /** RefreshToken 저장 (Redis: refresh:<memberId>) */
    public void storeRefreshTokenInRedis(Long memberId, String refreshToken) {
        String key = refreshKey(memberId);
        redisTemplate.opsForValue().set(key, refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    /** RefreshToken 조회 */
    public Optional<String> getRefreshTokenFromRedis(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(refreshKey(memberId)));
    }

    /** RefreshToken 제거 */
    public void removeRefreshTokenFromRedis(Long memberId) {
        redisTemplate.delete(refreshKey(memberId));
    }

    /**
     * Access Token 블랙리스트 등록
     * - jti/해시 키 관리, TTL 설정은 JwtService가 처리
     */
    public void blacklistAccessToken(String accessToken) {
        jwtService.blacklistAccessToken(accessToken);
    }

    /**
     * 쿠키의 RefreshToken을 검증하고 memberId 반환
     * 1) 쿠키에서 추출
     * 2) JWT 유효성 검사(만료/서명/블랙리스트)
     * 3) Redis 저장값과 일치 여부 확인
     */
    public Long getMemberIdFromRefreshToken(HttpServletRequest request) {
        String refreshToken = jwtService.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new ServiceException(ErrorCode.TOKEN_INVALID));

        Claims claims = jwtService.parseAndValidateToken(refreshToken);
        Long memberId = Optional.ofNullable(claims.get("memberId", Number.class))
                .map(Number::longValue)
                .orElseGet(() -> Long.parseLong(claims.getSubject()));

        String stored = redisTemplate.opsForValue().get(refreshKey(memberId));
        if (stored == null || !stored.equals(refreshToken)) {
            throw new ServiceException(ErrorCode.TOKEN_INVALID);
        }
        return memberId;
    }

    /** Refresh 쿠키 제거(로그아웃/비번변경 후 호출) */
    public void clearRefreshCookie(HttpServletResponse response) {
        jwtService.clearRefreshTokenCookie(response);
    }

    private static String refreshKey(Long memberId) {
        return "refresh:" + memberId;
    }
}
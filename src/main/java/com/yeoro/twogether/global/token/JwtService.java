package com.yeoro.twogether.global.token;

import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

import static com.yeoro.twogether.global.constant.AppConstants.REFRESH_TOKEN;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKeyHex; // hex 문자열(256비트 이상 권장: 64자 이상)

    @Value("${jwt.access_expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh_expiration}")
    private Long refreshTokenExpiration;

    private final StringRedisTemplate redis;

    public JwtService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /* ======================
    토큰 생성 (sub/jti 추가, 기존 memberId 유지)
    ====================== */

    /** AccessToken 생성 */
    public String createAccessToken(Long memberId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId); // 기존 호환
        claims.put("typ", "access");
        return createToken(claims, accessTokenExpiration, memberId);
    }

    /** RefreshToken 생성 */
    public String createRefreshToken(Long memberId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId); // 기존 호환
        claims.put("typ", "refresh");
        return createToken(claims, refreshTokenExpiration, memberId);
    }

    /** JWT 생성 (표준 sub/jti 포함) */
    public String createToken(Map<String, Object> claims, Long expiration, Long memberId) {
        SecretKey key = getSecretKey(secretKeyHex);
        long nowMillis = System.currentTimeMillis();
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(memberId)) // sub
                .setId(jti)                           // jti
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ======================
    파싱/검증 (+ 블랙리스트 검사)
    ====================== */

    /** JWT 파싱 및 유효성 검증 (+ 블랙리스트 검사) */
    public Claims parseAndValidateToken(String token) {
        return parseClaimsInternal(token, /*checkBlacklist=*/true);
    }

    /** 남은 토큰 유효 시간(ms) — 블랙리스트 무시하고 순수 만료만 계산 */
    public long getRemainingTime(String token) {
        try {
            Claims claims = parseClaimsInternal(token, /*checkBlacklist=*/false);
            Date expiration = claims.getExpiration();
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (ServiceException e) {
            return 0;
        }
    }

    private Claims parseClaimsInternal(String token, boolean checkBlacklist) {
        try {
            SecretKey key = getSecretKey(secretKeyHex);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (checkBlacklist) {
                String blacklistKey = blacklistKey(extractJtiOrHash(token, claims));
                if (Boolean.TRUE.equals(redis.hasKey(blacklistKey))) {
                    throw new ServiceException(ErrorCode.ACCESS_TOKEN_BLACKLISTED);
                }
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new ServiceException(ErrorCode.TOKEN_EXPIRED, e);
        } catch (JwtException e) {
            throw new ServiceException(ErrorCode.TOKEN_INVALID, e);
        }
    }

    /* ======================
    Refresh Token (쿠키/Redis)
    ====================== */

    /** Refresh Token 쿠키 저장 */
    public void setRefreshTokenCookie(String token, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTokenExpiration / 1000) // 초 단위
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /** Refresh Token 쿠키 삭제 */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /** 쿠키에서 Refresh Token 추출 */
    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        for (var c : request.getCookies()) {
            if (REFRESH_TOKEN.equals(c.getName())) {
                return Optional.ofNullable(c.getValue());
            }
        }
        return Optional.empty();
    }

    /** Redis에 Refresh Token 저장 (TTL = refresh 만료) */
    public void storeRefreshToken(Long memberId, String refreshToken) {
        redis.opsForValue().set(refreshKey(memberId), refreshToken,
                Duration.ofMillis(refreshTokenExpiration));
    }

    /** Redis에서 Refresh Token 조회 */
    public Optional<String> loadRefreshToken(Long memberId) {
        return Optional.ofNullable(redis.opsForValue().get(refreshKey(memberId)));
    }

    /** Redis에서 Refresh Token 무효화(삭제) */
    public void invalidateRefreshToken(Long memberId) {
        redis.delete(refreshKey(memberId));
    }

    /* ======================
    Access Token 블랙리스트
    ====================== */

    /** Access Token 블랙리스트 등록 (남은 만료시간만큼 유지) */
    public void blacklistAccessToken(String token) {
        long ttlMs = getRemainingTime(token);
        if (ttlMs <= 0) return;

        Claims claims;
        try {
            // 블랙리스트 검사 없이 파싱(등록 자체가 목적)
            claims = parseClaimsInternal(token, /*checkBlacklist=*/false);
        } catch (ServiceException e) {
            // 이미 만료/유효하지 않으면 등록 불필요
            return;
        }

        String jtiOrHash = extractJtiOrHash(token, claims);
        redis.opsForValue().set(blacklistKey(jtiOrHash), "1", Duration.ofMillis(ttlMs));
    }

    /* ======================
    추출/유틸
    ====================== */

    /** Authorization: Bearer ... 에서 액세스 토큰 추출 */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /** 현재 요청 컨텍스트에서 액세스 토큰 추출 (없으면 null) */
    public String resolveAccessTokenFromContextOrRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return resolveToken(attrs.getRequest());
    }

    /** claims에서 memberId 가져오기 (sub/커스텀 양쪽 지원) */
    public Long extractMemberId(Claims claims) {
        Object v = claims.get("memberId");
        if (v instanceof Number n) return n.longValue();
        String sub = claims.getSubject();
        return (sub != null) ? Long.parseLong(sub) : null;
    }

    /** SecretKey (hex) → SecretKey */
    private SecretKey getSecretKey(String hex) {
        // HexFormat 사용: secretKeyHex는 64자(256비트) 이상 권장
        return Keys.hmacShaKeyFor(java.util.HexFormat.of().parseHex(hex));
    }

    private static String refreshKey(Long memberId) {
        return "refresh:" + memberId;
    }

    private static String blacklistKey(String jtiOrHash) {
        return "blacklist:" + jtiOrHash;
    }

    /** jti가 없으면 토큰 전문 SHA-256 해시로 대체 (구버전 호환) */
    private static String extractJtiOrHash(String token, Claims claims) {
        String jti = claims.getId();
        return (jti != null && !jti.isBlank()) ? jti : sha256(token);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
package com.yeoro.twogether.global.token;

import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Token 쌍 생성
     */
    public TokenPair createTokenPair(Long memberId,
                                     String nickname,
                                     Long partnerId,
                                     String partnerNickname,
                                     LocalDate relationshipStartDate) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("nickname", nickname);
        claims.put("partnerId", partnerId);
        claims.put("partnerNickname", partnerNickname);
        claims.put("relationshipStartDate",
                relationshipStartDate != null ? relationshipStartDate.toString() : null);

        String accessToken = jwtService.createToken(claims, accessExpiration);
        String refreshToken = jwtService.createToken(claims, refreshExpiration);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 클라이언트에 토큰 전달 (헤더 + 쿠키 + 바디 JSON)
     */
    public void sendTokensToClient(HttpServletRequest request,
                                   HttpServletResponse response,
                                   TokenPair tokenPair,
                                   Long memberId,
                                   String nickname,
                                   Long partnerId,
                                   String partnerNickname,
                                   LocalDate relationshipStartDate) {
            response.setHeader("Authorization", "Bearer " + tokenPair.getAccessToken());
            jwtService.setRefreshTokenCookie(tokenPair.getRefreshToken(), response);
            storeRefreshTokenInRedis(memberId, tokenPair.getRefreshToken());
    }


    /**
     * Redis에 RefreshToken 저장
     */
    public void storeRefreshTokenInRedis(Long memberId, String refreshToken) {
        String key = "refresh:" + memberId;
        redisTemplate.opsForValue().set(key, refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
    }


    /**
     * Redis에서 RefreshToken 조회
     */
    public Optional<String> getRefreshTokenFromRedis(Long memberId) {
        String key = "refresh:" + memberId;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    /**
     * Redis에서 RefreshToken 제거
     */
    public void removeRefreshTokenFromRedis(Long memberId) {
        String key = "refresh:" + memberId;
        redisTemplate.delete(key);
    }

    public void blacklistAccessToken(String accessToken) {
        long remainingTime = jwtService.getRemainingTime(accessToken);
        if (remainingTime > 0) {
            String key = "blacklist:" + accessToken;
            redisTemplate.opsForValue().set(key, "logout", remainingTime, TimeUnit.MILLISECONDS);
        }
    }

    public Long getMemberIdFromRefreshToken(HttpServletRequest request) {
        // 쿠키에서 RefreshToken 추출
        String refreshToken = jwtService.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new ServiceException(ErrorCode.TOKEN_INVALID));

        // JWT 파싱 및 유효성 검증
        Claims claims = jwtService.parseAndValidateToken(refreshToken);
        Long memberId = claims.get("memberId", Number.class).longValue();

        // Redis에 저장된 RefreshToken과 일치하는지 확인
        String key = "refresh:" + memberId;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new ServiceException(ErrorCode.TOKEN_INVALID);
        }

        // 최종 memberId 반환
        return memberId;
    }
}
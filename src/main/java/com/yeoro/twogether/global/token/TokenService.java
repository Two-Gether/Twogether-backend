package com.yeoro.twogether.global.token;

import com.yeoro.twogether.global.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.yeoro.twogether.global.exception.ErrorCode.TOKEN_SEND_ERROR;


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
    public TokenPair createTokenPair(Long memberId, String nickname, Long partnerId, String partnerNickname) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("nickname", nickname);
        claims.put("partnerId", partnerId);
        claims.put("partnerNickname", partnerNickname);

        String accessToken = jwtService.createToken(claims, accessExpiration);
        String refreshToken = jwtService.createToken(claims, refreshExpiration);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 클라이언트에 토큰 전달 (헤더 + 쿠키 + 바디 JSON)
     */
    public void sendTokensToClient(
            HttpServletRequest request,
            HttpServletResponse response,
            TokenPair tokenPair,
            Long memberId,
            String nickname,
            Long partnerId,
            String partnerNickname
    ) {
        try {
            // 1. 헤더에 AccessToken 추가
            response.setHeader("Authorization", "Bearer " + tokenPair.getAccessToken());

            // 2. RefreshToken은 HttpOnly 쿠키로 설정
            jwtService.setRefreshTokenCookie(tokenPair.getRefreshToken(), response);

            // 3. RefreshToken → 세션
            storeRefreshTokenInRedis(memberId, tokenPair.getRefreshToken());

            // 4. JSON 바디 응답
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("accessToken", tokenPair.getAccessToken());
            responseBody.put("memberId", memberId);
            responseBody.put("nickname", nickname);
            responseBody.put("partnerId", partnerId);
            responseBody.put("partnerNickname", partnerNickname);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getWriter(), responseBody);

        } catch (IOException e) {
            throw new ServiceException(TOKEN_SEND_ERROR);
        }
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
}
package com.yeoro.twogether.global.token;

import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static com.yeoro.twogether.global.constant.AppConstants.REFRESH_TOKEN;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * JWT 생성
     * @param claims 사용자 정보 (memberId, nickname 등)
     * @param expiration 만료 시간 (밀리초)
     */
    public String createToken(Map<String, Object> claims, Long expiration) {
        SecretKey key = getSecretKey(secretKey);
        long nowMillis = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * JWT 파싱 및 유효성 검증
     */
    public Claims parseAndValidateToken(String token) {
        try {
            SecretKey key = getSecretKey(secretKey);
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ServiceException(ErrorCode.TOKEN_EXPIRED, e);
        } catch (JwtException e) {
            throw new ServiceException(ErrorCode.TOKEN_INVALID, e);
        }
    }

    /**
     * HttpOnly Refresh Token 쿠키 생성 및 설정
     */
    public void setRefreshTokenCookie(String token, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * JWT 서명용 시크릿 키 생성
     */
    private SecretKey getSecretKey(String secretKey) {
        return Keys.hmacShaKeyFor(java.util.HexFormat.of().parseHex(secretKey));
    }

    /**
     * HTTP 요청 헤더에서 Bearer 토큰 추출
     */
    public String resolveToken(jakarta.servlet.http.HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT의 남은 유효시간(ms)을 계산하여 반환
     * 만료된 토큰이면 예외 발생 대신 0을 반환
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = parseAndValidateToken(token);
            Date expiration = claims.getExpiration();
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (ServiceException e) {
            return 0; // 유효하지 않은 토큰은 블랙리스트 처리 대상이 아님
        }
    }

}
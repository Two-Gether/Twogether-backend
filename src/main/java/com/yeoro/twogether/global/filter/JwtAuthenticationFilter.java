package com.yeoro.twogether.global.filter;

import com.yeoro.twogether.global.argumentResolver.CustomUserDetails;
import com.yeoro.twogether.global.constant.AppConstants;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.token.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String bearer = request.getHeader(AppConstants.AUTHORIZATION_HEADER);

        if (bearer == null || !bearer.startsWith(AppConstants.BEARER_PREFIX)) {
            // 토큰이 없거나 잘못된 경우 인증 없이 그냥 다음 필터로 진행
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = bearer.substring(AppConstants.BEARER_PREFIX.length());

        try {
            // Redis 블랙리스트 조회: 로그아웃된 토큰인지 확인
            String blacklistKey = "blacklist:" + accessToken;
            String blacklisted = redisTemplate.opsForValue().get(blacklistKey);
            if ("logout".equals(blacklisted)) {
                throw new ServiceException(ErrorCode.ACCESS_TOKEN_BLACKLISTED);
            }

            // JWT 파싱 및 유효성 검증
            Claims claims = jwtService.parseAndValidateToken(accessToken);

            // memberId 추출 후 CustomUserDetails 생성 → Spring Security 인증 객체 설정
            Long memberId = claims.get("memberId", Long.class);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(new CustomUserDetails(memberId), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (ServiceException e) {
            log.warn("Token Error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT 처리 중 예상치 못한 오류", e);
        }

        filterChain.doFilter(request, response);
    }
}

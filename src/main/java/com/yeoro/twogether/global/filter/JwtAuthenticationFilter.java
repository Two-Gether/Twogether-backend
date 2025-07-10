package com.yeoro.twogether.global.filter;

import com.yeoro.twogether.global.argumentResolver.CustomUserDetails;
import com.yeoro.twogether.global.constant.AppConstants;
import com.yeoro.twogether.global.exception.ServiceException;
import com.yeoro.twogether.global.token.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

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
            Claims claims = jwtService.parseAndValidateToken(accessToken);
            Long memberId = claims.get("memberId", Long.class);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(memberId), null,
                    List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ServiceException e) {
            // 토큰 만료, 변조 등 검증 실패 시 로그 출력 후 인증 없이 진행
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT processing", e);
        }

        filterChain.doFilter(request, response);
    }
}

package com.yeoro.twogether.global.filter;

import static com.yeoro.twogether.global.exception.ErrorCode.TOKEN_EXTRACT_FAILED;

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

        String accessToken = refineAccessToken(request);

        Claims claims = jwtService.parseAndValidateToken(accessToken);

        Long memberId = claims.get("memberId", Long.class);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails(memberId), null,
                List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private String refineAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (bearer != null && bearer.startsWith(AppConstants.BEARER_PREFIX)) {
            return bearer.substring(AppConstants.BEARER_PREFIX.length());
        }
        throw new ServiceException(TOKEN_EXTRACT_FAILED);
    }
}

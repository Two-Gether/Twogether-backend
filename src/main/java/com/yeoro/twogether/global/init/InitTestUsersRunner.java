package com.yeoro.twogether.global.init;

import com.yeoro.twogether.domain.member.dto.request.LoginRequest;
import com.yeoro.twogether.domain.member.dto.request.SignupRequest;
import com.yeoro.twogether.domain.member.dto.response.LoginResponse;
import com.yeoro.twogether.domain.member.entity.Gender;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.repository.MemberRepository;
import com.yeoro.twogether.domain.member.service.EmailVerificationService;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.global.constant.AppConstants;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.init.test-users", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class InitTestUsersRunner implements ApplicationRunner {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        TestUser u1 = new TestUser("testUser1", "testUser1@example.com", "Aa123456!");
        TestUser u2 = new TestUser("testUser2", "testUser2@example.com", "Bb123456!");

        TokenBundle t1 = ensureUserAndIssueTokens(u1);
        TokenBundle t2 = ensureUserAndIssueTokens(u2);

        printTopBanner(t1, t2);
    }

    private TokenBundle ensureUserAndIssueTokens(TestUser u) {
        // 1) 이메일 인증 완료 상태로 강제 세팅
        emailVerificationService.markVerifiedForInit(u.email());

        // 2) 계정 없으면 회원가입, 있으면 관계 초기화
        Member member = memberRepository.findByEmail(u.email()).orElse(null);
        if (member == null) {
            MockHttpServletResponse signupResp = new MockHttpServletResponse();

            // SignupRequest(email, password, name, phoneNumber, gender, ageRange)  ← birthday 제거 반영
            SignupRequest req = new SignupRequest(
                    u.email(),
                    u.password(),
                    u.name(),
                    null,              // phoneNumber
                    Gender.UNKNOWN,    // gender
                    null               // ageRange
            );

            memberService.signup(req, signupResp);
            emailVerificationService.clearVerificationInfo(u.email()); // 남은 인증 데이터 정리
        } else if (member.getPartner() != null) {
            // 서비스 로직으로 연인 해제(양쪽 nickname null 포함)
            memberService.disconnectPartner(member.getId());
        }

        // 3) 로그인 해서 토큰 발급
        MockHttpServletRequest loginReq = new MockHttpServletRequest();
        MockHttpServletResponse loginResp = new MockHttpServletResponse();
        LoginResponse login = memberService.login(
                new LoginRequest(u.email(), u.password()),
                loginReq,
                loginResp
        );

        String access = login.accessToken();
        String refresh = extractRefreshToken(loginResp);

        return new TokenBundle(u.name(), u.email(), access, refresh);
    }

    /**
     * Refresh Token은 HttpOnly Set-Cookie 헤더로 내려가므로
     * MockHttpServletResponse#getHeaders("Set-Cookie")에서 파싱해야 한다.
     */
    private String extractRefreshToken(MockHttpServletResponse response) {
        // 1) Set-Cookie 헤더 우선 시도
        for (String header : response.getHeaders("Set-Cookie")) {
            // 예: refreshToken=xxxx; Max-Age=...; Path=/; Secure; HttpOnly; SameSite=None
            String[] parts = header.split(";", 2);
            String[] kv = parts[0].split("=", 2);
            String name = kv[0].trim();
            String value = (kv.length > 1) ? kv[1] : "";
            if (AppConstants.REFRESH_TOKEN.equals(name)) {
                return value;
            }
        }

        // 2) 혹시라도 Mock에 쿠키로 들어오는 경우 대비
        Cookie[] cookies = response.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AppConstants.REFRESH_TOKEN.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }

        return "(refresh not found)";
    }

    private void printTopBanner(TokenBundle t1, TokenBundle t2) {
        log.error("\n" +
                        "================================= TEST USERS =================================\n" +
                        " USER: {}  | EMAIL: {}\n" +
                        "   ACCESS : {}\n" +
                        "   REFRESH: {}\n" +
                        "------------------------------------------------------------------------------\n" +
                        " USER: {}  | EMAIL: {}\n" +
                        "   ACCESS : {}\n" +
                        "   REFRESH: {}\n" +
                        "===============================================================================\n",
                t1.name(), t1.email(), t1.accessToken(), t1.refreshToken(),
                t2.name(), t2.email(), t2.accessToken(), t2.refreshToken()
        );
    }

    private record TestUser(String name, String email, String password) {}
    private record TokenBundle(String name, String email, String accessToken, String refreshToken) {}
}

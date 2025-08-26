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

        // 2) 계정 없으면 회원가입
        Member member = memberRepository.findByEmail(u.email()).orElse(null);
        if (member == null) {
            MockHttpServletResponse signupResp = new MockHttpServletResponse();
            SignupRequest req = new SignupRequest(
                    u.email(),
                    u.password(),
                    u.nickname(),
                    null,
                    null,
                    Gender.UNKNOWN,
                    null
            );
            memberService.signup(req, signupResp);
            emailVerificationService.clearVerificationInfo(u.email()); // 남은 인증 데이터 정리
        } else if (member.getPartner() != null) {
            // 연인 연동 제거 보장
            member.connectPartner(null);
            memberRepository.save(member);
        }

        // 3) 로그인 해서 토큰 발급
        MockHttpServletRequest loginReq = new MockHttpServletRequest();
        MockHttpServletResponse loginResp = new MockHttpServletResponse();
        LoginResponse login = memberService.login(new LoginRequest(u.email(), u.password()), loginReq, loginResp);

        String access = login.accessToken();
        String refresh = extractRefreshToken(loginResp);

        return new TokenBundle(u.nickname(), u.email(), access, refresh);
    }

    private String extractRefreshToken(MockHttpServletResponse response) {
        if (response.getCookies() != null) {
            for (Cookie c : response.getCookies()) {
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
                t1.nickname(), t1.email(), t1.accessToken(), t1.refreshToken(),
                t2.nickname(), t2.email(), t2.accessToken(), t2.refreshToken()
        );
    }

    private record TestUser(String nickname, String email, String password) {}
    private record TokenBundle(String nickname, String email, String accessToken, String refreshToken) {}
}

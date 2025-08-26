package com.yeoro.twogether.domain.member.service;

import com.yeoro.twogether.domain.member.mail.MailService;
import com.yeoro.twogether.global.util.CodeGenerator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MailService mailService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "email_verification:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public void sendVerificationCode(String email) throws MessagingException {
        String code = CodeGenerator.generateEmailCode();
        mailService.sendSimpleMessage(email, code);

        redisTemplate.opsForHash().put(PREFIX + email, "code", code);
        redisTemplate.opsForHash().put(PREFIX + email, "isVerified", false);
        redisTemplate.expire(PREFIX + email, TTL);
    }

    public void verifyCode(String email, String inputCode) {
        String key = PREFIX + email;
        String savedCode = (String) redisTemplate.opsForHash().get(key, "code");

        if (savedCode == null || !savedCode.equals(inputCode)) {
            throw new IllegalArgumentException("인증 코드가 유효하지 않거나 일치하지 않습니다.");
        }

        redisTemplate.opsForHash().put(key, "isVerified", true);
        redisTemplate.expire(key, TTL);
    }

    public boolean isVerified(String email) {
        Object result = redisTemplate.opsForHash().get(PREFIX + email, "isVerified");
        return result instanceof Boolean && (Boolean) result;
    }

    public void clearVerificationInfo(String email) {
        redisTemplate.delete(PREFIX + email);
    }

    // 테스트용 데이터 추가를 위한 메서드: 이메일 인증을 건너뛰고 강제로 "인증 완료" 상태
    public void markVerifiedForInit(String email) {
        String key = PREFIX + email;
        redisTemplate.opsForHash().put(key, "isVerified", true);

        // code 필드가 비어 있으면 dummy 값 넣어줌
        if (redisTemplate.opsForHash().get(key, "code") == null) {
            redisTemplate.opsForHash().put(key, "code", "INIT");
        }

        redisTemplate.expire(key, TTL);
    }

}

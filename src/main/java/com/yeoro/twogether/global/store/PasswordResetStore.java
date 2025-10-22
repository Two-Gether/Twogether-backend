package com.yeoro.twogether.global.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetStore {
    private final StringRedisTemplate redis;

    private static final String CODE_KEY    = "pwdreset:code:";      // email -> code
    private static final String ATTEMPT_KEY = "pwdreset:attempts:";  // email -> int
    private static final String TICKET_KEY  = "pwdreset:ticket:";    // email -> reset ticket

    private static final Duration CODE_TTL    = Duration.ofMinutes(10);
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(15);
    private static final Duration TICKET_TTL  = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS     = 5;

    private static String norm(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // ----- CODE -----
    public void saveCode(String email, String code) {
        redis.opsForValue().set(CODE_KEY + norm(email), code, CODE_TTL);
    }
    public String getCode(String email) {
        return redis.opsForValue().get(CODE_KEY + norm(email));
    }
    public void deleteCode(String email) {
        redis.delete(CODE_KEY + norm(email));
    }

    // ----- ATTEMPTS -----
    /** 증가 후 과다 여부 반환(true면 차단) */
    public boolean tooManyAttempts(String email) {
        String key = ATTEMPT_KEY + norm(email);
        Long cnt = redis.opsForValue().increment(key);
        if (cnt != null && cnt == 1L) {
            redis.expire(key, ATTEMPT_TTL);
        }
        return cnt != null && cnt > MAX_ATTEMPTS;
    }
    public void clearAttempts(String email) {
        redis.delete(ATTEMPT_KEY + norm(email));
    }

    // ----- RESET TICKET -----
    /** 코드 검증 성공 시 발급할 일회용 티켓 생성 & 저장(10분 TTL) */
    public String issueTicket(String email) {
        String ticket = UUID.randomUUID().toString();
        redis.opsForValue().set(TICKET_KEY + norm(email), ticket, TICKET_TTL);
        return ticket;
    }

    /** 일회성 검증/소비(성공 시 true, 저장된 티켓 삭제) */
    public boolean consumeTicket(String email, String ticket) {
        String key = TICKET_KEY + norm(email);
        String saved = redis.opsForValue().get(key);
        if (saved == null || !saved.equals(ticket)) return false;
        redis.delete(key);
        return true;
    }
}
package com.yeoro.twogether.global.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PartnerCodeStore {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "partner:code:";
    private static final long DEFAULT_TTL_SECONDS = 180L; // 기본 TTL: 3분

    /**
     * 파트너 코드 저장 (기본 TTL 적용)
     */
    public void save(String code, Long memberId) {
        save(code, memberId, DEFAULT_TTL_SECONDS);
    }

    /**
     * 파트너 코드 저장 (커스텀 TTL)
     */
    public void save(String code, Long memberId, long ttlSeconds) {
        redisTemplate.opsForValue().set(PREFIX + code, String.valueOf(memberId), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 파트너 코드 존재 여부 확인 (중복 방지용)
     */
    public boolean exists(String code) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + code));
    }

    /**
     * 파트너 코드 사용 (1회성 → 사용 후 삭제)
     */
    public Long consume(String code) {
        String key = PREFIX + code;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            redisTemplate.delete(key); // 사용 후 제거
            return Long.valueOf(value);
        }
        return null;
    }
}

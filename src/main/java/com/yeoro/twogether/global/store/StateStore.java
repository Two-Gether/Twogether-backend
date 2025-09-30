package com.yeoro.twogether.global.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StateStore {private final StringRedisTemplate redis;
    private static final Duration TTL = Duration.ofMinutes(5);

    public void save(String state, String returnUrl) {
        redis.opsForValue().set("state:"+state, returnUrl, TTL);
    }
    public Optional<String> consume(String state) {
        String k = "state:"+state;
        String v = redis.opsForValue().get(k);
        if (v != null) redis.delete(k);
        return Optional.ofNullable(v);
    }
}
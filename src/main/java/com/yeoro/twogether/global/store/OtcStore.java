package com.yeoro.twogether.global.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OtcStore {private final StringRedisTemplate redis;
    private final ObjectMapper om;
    private static final Duration TTL = Duration.ofSeconds(60);

    public record OtcExchangeRequest(String otc) {}
    private record Payload(Long memberId) {}

    public String issue(Long memberId) {
        String otc = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(key(otc), write(new Payload(memberId)), TTL);
        return otc;
    }
    public Optional<Long> consume(String otc) {
        String k = key(otc);
        String v = redis.opsForValue().get(k);
        if (v == null) return Optional.empty();
        redis.delete(k);
        try {
            Payload p = om.readValue(v, new TypeReference<Payload>() {});
            return Optional.ofNullable(p.memberId());
        } catch (Exception e) { return Optional.empty(); }
    }
    private String key(String otc) { return "otc:"+otc; }
    private String write(Object o) { try { return om.writeValueAsString(o);} catch(Exception e){ throw new RuntimeException(e); } }
}
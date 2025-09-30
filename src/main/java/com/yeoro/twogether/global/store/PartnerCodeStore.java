package com.yeoro.twogether.global.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PartnerCodeStore {

    private final StringRedisTemplate redisTemplate;

    /** code -> memberId */
    private static final String CODE_PREFIX  = "partner:code:";
    /** memberId -> code (역인덱스) */
    private static final String OWNER_PREFIX = "partner:owner:";
    /** 분산락 키 prefix (선택) */
    private static final String LOCK_PREFIX  = "lock:partner:code:";

    /** TTL: 3분 */
    private static final long DEFAULT_TTL_SECONDS = 180L;

    /** 이미 발급된 코드 조회(있으면 재사용) */
    public String findCodeByMember(Long memberId) {
        return redisTemplate.opsForValue().get(OWNER_PREFIX + memberId);
    }

    /** 코드 존재(중복) 확인 */
    public boolean existsCode(String code) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CODE_PREFIX + code));
    }

    /** 양방향 저장(기본 TTL) */
    public void saveBoth(String code, Long memberId) {
        saveBoth(code, memberId, DEFAULT_TTL_SECONDS);
    }

    /**
     * 양방향 저장(커스텀 TTL)
     * - 덮어쓰기 방지: setIfAbsent(NX)
     * - code 충돌 시 ownerKey 롤백
     */
    public void saveBoth(String code, Long memberId, long ttlSeconds) {
        final String codeKey  = CODE_PREFIX + code;
        final String ownerKey = OWNER_PREFIX + memberId;

        // 1) memberId -> code 먼저 고정 (이미 있으면 재사용)
        Boolean ownerSet = redisTemplate.opsForValue()
                .setIfAbsent(ownerKey, code, ttlSeconds, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(ownerSet)) {
            // 이미 소유 코드가 있으면 그대로 종료
            return;
        }

        // 2) code -> memberId 고정 (충돌 시 롤백)
        Boolean codeSet = redisTemplate.opsForValue()
                .setIfAbsent(codeKey, String.valueOf(memberId), ttlSeconds, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(codeSet)) {
            // code가 누군가 선점했다면 역인덱스 롤백
            redisTemplate.delete(ownerKey);
            throw new IllegalStateException("Code collision detected while saving mapping.");
        }
    }

    /** 분산락(선택) — 동일 memberId 동시 발급 방지용 */
    public boolean tryLock(Long memberId, long ttlSeconds) {
        String lockKey = LOCK_PREFIX + memberId;
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ttlSeconds, TimeUnit.SECONDS)
        );
    }

    /** 분산락 해제 */
    public void unlock(Long memberId) {
        redisTemplate.delete(LOCK_PREFIX + memberId);
    }

    /**
     * 코드 소비(1회성) — 양방향 삭제
     * @return memberId (없으면 null)
     */
    public Long consume(String code) {
        String codeKey = CODE_PREFIX + code;
        String memberIdStr = redisTemplate.opsForValue().get(codeKey);
        if (memberIdStr == null) return null;

        Long memberId = Long.valueOf(memberIdStr);
        String ownerKey = OWNER_PREFIX + memberId;

        // 간단 삭제(강한 원자성이 필요하면 Lua 스크립트 적용 가능)
        redisTemplate.delete(codeKey);
        redisTemplate.delete(ownerKey);
        return memberId;
    }

    /** 강제 만료(취소) — 양방향 삭제 */
    public void invalidateByMember(Long memberId) {
        String ownerKey = OWNER_PREFIX + memberId;
        String code = redisTemplate.opsForValue().get(ownerKey);
        if (code != null) {
            redisTemplate.delete(ownerKey);
            redisTemplate.delete(CODE_PREFIX + code);
        }
    }
}

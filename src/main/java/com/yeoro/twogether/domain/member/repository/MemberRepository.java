package com.yeoro.twogether.domain.member.repository;

import com.yeoro.twogether.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.nio.channels.FileChannel;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    boolean existsByPlatformId(String platformId);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPlatformId(String platformId);
}

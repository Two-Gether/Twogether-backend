package com.yeoro.twogether.domain.member.repository;

import com.yeoro.twogether.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    boolean existsByPlatformId(String platformId);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPlatformId(String platformId);

    @Query("select m from Member m left join fetch m.partner where m.id = :id")
    Optional<Member> findByIdWithPartner(@Param("id") Long id);

    /** 상대가 나를 partner로 들고 있는 경우(역참조) 찾기 */
    Optional<Member> findByPartner_Id(Long partnerId);

}

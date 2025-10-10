package com.yeoro.twogether.domain.place.repository;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.place.entity.Place;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // ===== 기존 =====
    boolean existsByMemberAndAddress(Member member, String address);

    @EntityGraph(attributePaths = "tags")
    List<Place> findAllByAddress(String address);

    @EntityGraph(attributePaths = "tags")
    Optional<Place> findByIdAndMember_Id(Long placeId, Long memberId);

    @EntityGraph(attributePaths = "tags")
    Optional<Place> findByMember_IdAndAddress(Long memberId, String address);

    boolean existsByMemberAndAddressAndCreatedAtBetween(
            Member member,
            String address,
            java.time.LocalDateTime startInclusive,
            java.time.LocalDateTime endExclusive
    );

    @EntityGraph(attributePaths = "tags")
    List<Place> findAllByMember_IdInAndCreatedAtBetween(
            List<Long> memberIds,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    // ===== 회원 삭제용 =====

    /** 회원이 올린 Place 전체 조회 (S3 키 수집용) */
    @EntityGraph(attributePaths = "tags") // 필요 없으면 제거 가능
    List<Place> findAllByMember_Id(Long memberId);

    /** 회원이 올린 Place의 ID만 조회 (place_tags 선삭제용) */
    @Query("select p.id from Place p where p.member.id = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    /** ElementCollection 테이블(place_tags) 벌크 삭제 - 네이티브 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from place_tags where place_id in (:placeIds)", nativeQuery = true)
    int deleteTagsByPlaceIds(@Param("placeIds") List<Long> placeIds);

    /** Place 벌크 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Place p where p.member.id = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}

package com.yeoro.twogether.domain.place.repository;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.place.entity.Place;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    boolean existsByMemberAndAddress(Member member, String address);

    @EntityGraph(attributePaths = "tags")
    List<Place> findAllByAddress(String address);
    @EntityGraph(attributePaths = "tags")
    Optional<Place> findByIdAndMember_Id(Long placeId, Long memberId);
    @EntityGraph(attributePaths = "tags")
    Optional<Place> findByMember_IdAndAddress(Long memberId, String address);
}
package com.yeoro.twogether.domain.waypoint.repository;

import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WaypointRepository extends JpaRepository<Waypoint, Long> {

    @Query("select w from Waypoint w where w.member.id in :memberIds")
    List<Waypoint> findByMemberIds(@Param("memberIds") List<Long> memberIds);

    @Query("select w.id from Waypoint w where w.member.id = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Waypoint w where w.member.id = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}

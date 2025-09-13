package com.yeoro.twogether.domain.waypoint.repository;

import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WaypointItemRepository extends JpaRepository<WaypointItem, Long> {

    List<WaypointItem> findWaypointItemsByWaypointId(Long waypointId);

    void deleteByWaypointId(Long waypointId);

    @Query("SELECT COALESCE(MAX(w.itemOrder), 0) FROM WaypointItem w WHERE w.waypoint.id = :waypointId")
    int findMaxOrderByWaypointId(@Param("waypointId") Long waypointId);

    List<WaypointItem> findByWaypointOrderByItemOrderAsc(Waypoint waypoint);

    @Query("SELECT wi.waypoint.id, COUNT(wi) " +
        "FROM WaypointItem wi " +
        "WHERE wi.waypoint.member.id = :memberId " +
        "GROUP BY wi.waypoint.id")
    List<Object[]> countItemsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT w FROM WaypointItem w WHERE w.waypoint.id = :waypointId ORDER BY w.itemOrder ASC")
    List<WaypointItem> findTop3ByWaypointIdOrderByItemOrderAsc(@Param("waypointId") Long waypointId,
        Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WaypointItem wi where wi.waypoint.id in (:waypointIds)")
    int deleteByWaypointIds(@Param("waypointIds") List<Long> waypointIds);

    List<WaypointItem> findAllByWaypoint_IdIn(List<Long> waypointIds);
}

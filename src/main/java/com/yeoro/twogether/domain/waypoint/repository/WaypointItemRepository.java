package com.yeoro.twogether.domain.waypoint.repository;

import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaypointItemRepository extends JpaRepository<WaypointItem, Long> {

    List<WaypointItem> findWaypointItemsByWaypointId(Long waypointId);

    void deleteByWaypointId(Long waypointId);

    @Query("SELECT COALESCE(MAX(w.itemOrder), 0) FROM WaypointItem w WHERE w.waypoint.id = :waypointId")
    int findMaxOrderByWaypointId(@Param("waypointId") Long waypointId);

    @Modifying
    @Query("UPDATE WaypointItem w SET w.itemOrder = w.itemOrder - 1 WHERE w.waypoint.id = :waypointId AND w.itemOrder > :deletedOrder")
    void decreaseOrderAfter(@Param("waypointId") Long waypointId,
        @Param("deletedOrder") Integer deletedOrder);
}

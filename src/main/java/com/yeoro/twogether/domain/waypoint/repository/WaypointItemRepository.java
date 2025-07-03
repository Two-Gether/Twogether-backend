package com.yeoro.twogether.domain.waypoint.repository;

import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaypointItemRepository extends JpaRepository<WaypointItem, Long> {
    List<WaypointItem> findWaypointItemsByWaypointId(Long waypointId);
}

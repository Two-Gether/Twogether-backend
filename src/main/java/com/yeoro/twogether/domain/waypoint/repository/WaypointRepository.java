package com.yeoro.twogether.domain.waypoint.repository;

import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaypointRepository extends JpaRepository<Waypoint, Long> {

    List<Waypoint> findWaypointByMemberId(Long memberId);
}

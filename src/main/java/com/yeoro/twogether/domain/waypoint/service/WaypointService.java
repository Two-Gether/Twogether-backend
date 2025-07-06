package com.yeoro.twogether.domain.waypoint.service;

import com.yeoro.twogether.domain.waypoint.dto.WaypointItemSummaryListResponse;

public interface WaypointService {
    Long createWaypoint(Long memberId, String name);
    WaypointItemSummaryListResponse getWaypointSummaryList(Long memberId, Long waypointId);
    Long updateWaypoint(Long memberId, Long waypointId, String name);
    void deleteWaypoint(Long memberId, Long waypointId);
}

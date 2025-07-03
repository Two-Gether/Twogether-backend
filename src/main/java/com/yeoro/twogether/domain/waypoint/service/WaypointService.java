package com.yeoro.twogether.domain.waypoint.service;

import com.yeoro.twogether.domain.waypoint.dto.WaypointItemSummaryListResponse;

public interface WaypointService {
    Long createWaypoint(String name);
    WaypointItemSummaryListResponse getWaypointSummaryList(Long id);
    Long updateWaypoint(Long id, String name);
    void deleteWaypoint(Long id);
}

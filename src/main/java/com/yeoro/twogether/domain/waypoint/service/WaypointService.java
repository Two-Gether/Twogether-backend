package com.yeoro.twogether.domain.waypoint.service;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointCreateRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointUpdateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointWithItemsResponse;

public interface WaypointService {

    WaypointCreateResponse createWaypoint(Long memberId, WaypointCreateRequest request);

    WaypointWithItemsResponse getWaypoint(Long memberId, Long waypointId);

    WaypointUpdateResponse updateWaypoint(Long memberId, Long waypointId,
        WaypointUpdateRequest request);

    void deleteWaypoint(Long memberId, Long waypointId);
}

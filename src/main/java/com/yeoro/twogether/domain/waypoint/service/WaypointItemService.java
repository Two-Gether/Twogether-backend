package com.yeoro.twogether.domain.waypoint.service;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemReorderRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;

public interface WaypointItemService {

    WaypointItemCreateResponse addWaypointItem(Long memberId, Long waypointId,
        WaypointItemAddRequest request);

    void reorderWaypointItem(Long memberId, Long waypointId,
        WaypointItemReorderRequest request);

    void deleteWaypointItem(Long memberId, Long waypointId, Long waypointItemId);
}

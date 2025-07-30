package com.yeoro.twogether.domain.waypoint.service;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemDeleteRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemReorderRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemUpdateResponse;

public interface WaypointItemService {

    WaypointItemCreateResponse addWaypointItem(Long memberId, Long waypointId,
        WaypointItemAddRequest request);

    void reorderWaypointItem(Long memberId, Long waypointId,
        WaypointItemReorderRequest request);

    WaypointItemUpdateResponse updateWaypointItem(Long memberId, Long waypointId,
        Long waypointItemId, WaypointItemUpdateRequest request);

    void deleteWaypointItems(Long memberId, Long waypointId, WaypointItemDeleteRequest request);
}

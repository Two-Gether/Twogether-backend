package com.yeoro.twogether.domain.waypoint.service;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemCopyRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemDeleteRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemReorderRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemUpdateResponse;

public interface WaypointItemService {

    WaypointItemCreateResponse addWaypointItem(Long memberId, Long waypointId,
        WaypointItemAddRequest request);

    WaypointItemUpdateResponse updateWaypointItem(Long memberId, Long waypointId,
        Long waypointItemId, WaypointItemUpdateRequest request);

    void copyWaypointItems(Long memberId, Long waypointId, Long targetWaypointId,
        WaypointItemCopyRequest request);

    void reorderWaypointItem(Long memberId, Long waypointId,
        WaypointItemReorderRequest request);

    void deleteWaypointItems(Long memberId, Long waypointId, WaypointItemDeleteRequest request);
}

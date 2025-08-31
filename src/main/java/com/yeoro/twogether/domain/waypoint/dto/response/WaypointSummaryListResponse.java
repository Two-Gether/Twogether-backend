package com.yeoro.twogether.domain.waypoint.dto.response;

import java.util.List;

public record WaypointSummaryListResponse(
    List<WaypointSummaryResponse> waypointSummaryResponses
) {

}

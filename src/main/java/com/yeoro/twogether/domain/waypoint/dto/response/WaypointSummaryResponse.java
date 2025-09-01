package com.yeoro.twogether.domain.waypoint.dto.response;

public record WaypointSummaryResponse(
    Long waypointId,
    String name,
    Long itemCount
) {

}

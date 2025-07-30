package com.yeoro.twogether.domain.waypoint.dto.response;

import lombok.Builder;

@Builder
public record WaypointSummaryResponse(
    String name,
    String imageUrl,
    int order
) {

}

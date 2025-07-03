package com.yeoro.twogether.domain.waypoint.dto;

import lombok.Builder;

@Builder
public record WaypointItemSummaryResponse(
    String name,
    String imageUrl
) {

}

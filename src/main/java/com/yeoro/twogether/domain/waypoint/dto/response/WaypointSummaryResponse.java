package com.yeoro.twogether.domain.waypoint.dto.response;

import lombok.Builder;

@Builder
public record WaypointSummaryResponse(
    Long itemId,
    String name,
    String imageUrl,
    String memo,
    int order
) {

}

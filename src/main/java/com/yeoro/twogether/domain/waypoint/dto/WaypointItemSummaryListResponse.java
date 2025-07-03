package com.yeoro.twogether.domain.waypoint.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record WaypointItemSummaryListResponse(
    String waypointName,
    List<WaypointItemSummaryResponse> waypointItemSummaryResponses
) {
}

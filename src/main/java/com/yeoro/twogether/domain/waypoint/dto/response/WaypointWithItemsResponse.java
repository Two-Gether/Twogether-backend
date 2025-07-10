package com.yeoro.twogether.domain.waypoint.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record WaypointWithItemsResponse(
    String waypointName,
    List<WaypointSummaryResponse> waypointSummaryResponses
) {

}

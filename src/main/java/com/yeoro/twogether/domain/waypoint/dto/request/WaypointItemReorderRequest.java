package com.yeoro.twogether.domain.waypoint.dto.request;

import java.util.List;

public record WaypointItemReorderRequest(
    List<Long> orderedIds
) {

}

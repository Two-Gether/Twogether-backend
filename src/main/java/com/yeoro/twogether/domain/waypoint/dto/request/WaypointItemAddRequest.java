package com.yeoro.twogether.domain.waypoint.dto.request;

public record WaypointItemAddRequest(
    String name,
    String address,
    String imageUrl
) {

}

package com.yeoro.twogether.domain.waypoint.service;

public interface WaypointItemService {
    Long addWaypointItem(Long waypointId, String name,  String address, String imageUrl);
    void deleteWaypointItem(Long waypointItemId);
}

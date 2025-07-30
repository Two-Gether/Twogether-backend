package com.yeoro.twogether.domain.waypoint.service.mapper;

import com.yeoro.twogether.domain.waypoint.dto.response.WaypointSummaryResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointWithItemsResponse;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WaypointMapper {

    /**
     * Waypoint 이름과 하위 WaypointItem 목록을 응답 DTO로 변환
     */
    public WaypointWithItemsResponse toWaypointWithItemsResponse(String waypointName,
        List<WaypointItem> waypointItems) {
        return WaypointWithItemsResponse.builder()
            .waypointName(waypointName)
            .waypointSummaryResponses(toWaypointSummaryResponseList(waypointItems))
            .build();
    }

    /**
     * WaypointItem 엔티티 목록을 WaypointSummaryResponse DTO 목록으로 변환
     */
    private List<WaypointSummaryResponse> toWaypointSummaryResponseList(
        List<WaypointItem> waypointItems) {
        return waypointItems.stream()
            .map(this::toWaypointItemSummaryResponse)
            .collect(Collectors.toList());
    }

    /**
     * 단일 WaypointItem 엔티티를 WaypointSummaryResponse DTO로 변환
     */
    private WaypointSummaryResponse toWaypointItemSummaryResponse(WaypointItem waypointItem) {
        return WaypointSummaryResponse.builder()
            .name(waypointItem.getName())
            .imageUrl(waypointItem.getImageUrl())
            .order(waypointItem.getItemOrder())
            .build();
    }


}

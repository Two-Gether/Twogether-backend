package com.yeoro.twogether.domain.waypoint.service.mapper;

import com.yeoro.twogether.domain.waypoint.dto.WaypointItemSummaryListResponse;
import com.yeoro.twogether.domain.waypoint.dto.WaypointItemSummaryResponse;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;


@Component
public class WaypointItemMapper {

    /**
     * Waypoint 이름과 하위 WaypointItem 목록을 응답 DTO로 변환
     */
    public WaypointItemSummaryListResponse toWaypointItemSummaryListResponse(String waypointName, List<WaypointItem> waypointItems) {
        return WaypointItemSummaryListResponse.builder()
            .waypointName(waypointName)
            .waypointItemSummaryResponses(toWaypointItemSummaryResponseList(waypointItems))
            .build();
    }

    /**
     * WaypointItem 엔티티 목록을 WaypointItemSummaryResponse DTO 목록으로 변환
     */
    private List<WaypointItemSummaryResponse> toWaypointItemSummaryResponseList(List<WaypointItem> waypointItems) {
        return waypointItems.stream()
            .map(this::toWaypointItemSummaryResponse)
            .collect(Collectors.toList());
    }

    /**
     * 단일 WaypointItem 엔티티를 WaypointItemSummaryResponse DTO로 변환
     */
    private WaypointItemSummaryResponse toWaypointItemSummaryResponse(WaypointItem waypointItem) {
        return WaypointItemSummaryResponse.builder()
            .name(waypointItem.getName())
            .imageUrl(waypointItem.getImageUrl())
            .build();
    }




}

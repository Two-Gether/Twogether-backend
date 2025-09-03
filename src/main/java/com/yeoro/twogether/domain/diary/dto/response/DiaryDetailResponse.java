package com.yeoro.twogether.domain.diary.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record DiaryDetailResponse(
    String title,
    LocalDate startDate,
    LocalDate endDate,
    StickerListResponse stickerListResponse,
    Long waypointId,
    String memo,
    WaypointItemTop3ListResponse waypointItemTop3ListResponse
) {

    @Builder
    public record WaypointItemTop3ListResponse(List<WaypointItemTop3Response> items) {

    }

    @Builder
    public record WaypointItemTop3Response(
        Long id,
        String name,
        String address,
        String imageUrl,
        String memo,
        int itemOrder
    ) {

    }
}

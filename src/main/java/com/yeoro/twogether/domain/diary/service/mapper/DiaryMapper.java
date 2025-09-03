package com.yeoro.twogether.domain.diary.service.mapper;

import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse.WaypointItemTop3ListResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse.WaypointItemTop3Response;
import com.yeoro.twogether.domain.diary.dto.response.StickerListResponse;
import com.yeoro.twogether.domain.diary.dto.response.StickerResponse;
import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.diary.entity.Sticker;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiaryMapper {

    public DiaryDetailResponse toDiaryDetailResponse(Diary diary, List<Sticker> stickers,
        List<WaypointItem> topWaypointItems) {
        return DiaryDetailResponse.builder()
            .title(diary.getTitle())
            .startDate(diary.getStartDate())
            .endDate(diary.getEndDate())
            .stickerListResponse(toStickerListResponse(stickers))
            .waypointId(diary.getWaypointId())
            .memo(diary.getMemo())
            .waypointItemTop3ListResponse(toWaypointItemTop3ListResponse(topWaypointItems))
            .build();
    }

    // === [Sticker Mapper In Diary] ===

    public StickerListResponse toStickerListResponse(List<Sticker> stickers) {
        List<StickerResponse> stickerResponseList = stickers.stream()
            .map(this::toStickerResponse)
            .toList();

        return new StickerListResponse(stickerResponseList);

    }

    public StickerResponse toStickerResponse(Sticker sticker) {
        return StickerResponse.builder()
            .id(sticker.getId())
            .imageUrl(sticker.getImageUrl())
            .main(sticker.isMain())
            .build();
    }

    // === [Top3 WaypointItem Mapper In Diary] ===

    public WaypointItemTop3ListResponse toWaypointItemTop3ListResponse(
        List<WaypointItem> waypointItems) {
        List<WaypointItemTop3Response> itemResponses = waypointItems.stream()
            .map(this::toWaypointItemTop3Response)
            .toList();

        return new WaypointItemTop3ListResponse(itemResponses);
    }

    public WaypointItemTop3Response toWaypointItemTop3Response(WaypointItem item) {
        return WaypointItemTop3Response.builder()
            .id(item.getId())
            .name(item.getName())
            .address(item.getAddress())
            .imageUrl(item.getImageUrl())
            .memo(item.getMemo())
            .itemOrder(item.getItemOrder())
            .build();
    }
}

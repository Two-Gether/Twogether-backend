package com.yeoro.twogether.domain.diary.dto.request;

import java.time.LocalDate;

public record DiaryCreateRequest(
    String title,
    LocalDate startDate,
    LocalDate endDate,
    StickerListRequest stickerListRequest,
    Long waypointId,
    String memo
) {

}

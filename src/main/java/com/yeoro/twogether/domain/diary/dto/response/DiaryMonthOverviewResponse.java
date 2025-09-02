package com.yeoro.twogether.domain.diary.dto.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record DiaryMonthOverviewResponse(
    String title,
    LocalDate startDate,
    LocalDate endDate,
    String mainStickerUrl
) {

}

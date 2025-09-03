package com.yeoro.twogether.domain.diary.dto.request;

import java.time.LocalDate;

public record DiaryMonthOverviewRequest(
    LocalDate startDate,
    LocalDate endDate
) {

}

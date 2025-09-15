package com.yeoro.twogether.domain.diary.service;

import com.yeoro.twogether.domain.diary.dto.request.DiaryCreateRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryUpdateRequest;
import com.yeoro.twogether.domain.diary.dto.response.DiaryCreateResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewListResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryUpdateResponse;
import java.time.LocalDate;

public interface DiaryService {

    DiaryCreateResponse createDiary(Long memberId, DiaryCreateRequest diaryCreateRequest);

    DiaryMonthOverviewListResponse getMonthOverviewDiary(Long memberId, LocalDate startDate,
        LocalDate endDate);

    DiaryDetailResponse getDetailDiary(Long memberId, Long diaryId);

    DiaryUpdateResponse updateDiary(Long memberId, Long diaryId,
        DiaryUpdateRequest diaryUpdateRequest);

    void deleteDiary(Long memberId, Long diaryId);
}

package com.yeoro.twogether.domain.diary.controller;

import com.yeoro.twogether.domain.diary.dto.request.DiaryCreateRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryMonthOverviewRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryUpdateRequest;
import com.yeoro.twogether.domain.diary.dto.response.DiaryCreateResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewListResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryUpdateResponse;
import com.yeoro.twogether.domain.diary.service.DiaryService;
import com.yeoro.twogether.global.argumentResolver.Login;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diary")
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public DiaryCreateResponse createDiary(
        @Login Long memberId,
        @RequestBody DiaryCreateRequest request) {
        return diaryService.createDiary(memberId, request);
    }

    @GetMapping
    public DiaryMonthOverviewListResponse getMonthOverviewDiary(
        @Login Long memberId,
        @RequestBody DiaryMonthOverviewRequest request) {
        return diaryService.getMonthOverviewDiary(memberId, request);
    }

    @GetMapping("/{diaryId}")
    public DiaryDetailResponse getDetailDiary(@Login Long memberId, @PathVariable Long diaryId) {
        return diaryService.getDetailDiary(memberId, diaryId);
    }

    @PatchMapping("/{diaryId}")
    public DiaryUpdateResponse updateDiary(
        @Login Long memberId,
        @PathVariable Long diaryId,
        @RequestBody DiaryUpdateRequest request) {
        return diaryService.updateDiary(memberId, diaryId, request);
    }

    @DeleteMapping("/{diaryId}")
    public void deleteDiary(@Login Long memberId, @PathVariable Long diaryId) {
        diaryService.deleteDiary(memberId, diaryId);
    }

}

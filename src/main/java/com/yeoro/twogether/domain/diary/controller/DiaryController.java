package com.yeoro.twogether.domain.diary.controller;

import com.yeoro.twogether.domain.diary.dto.request.DiaryCreateRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryUpdateRequest;
import com.yeoro.twogether.domain.diary.dto.response.DiaryCreateResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewListResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryUpdateResponse;
import com.yeoro.twogether.domain.diary.service.DiaryService;
import com.yeoro.twogether.global.argumentResolver.Login;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diary 관련 CRUD 요청을 처리하는 REST 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diary")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 새로운 Diary를 생성합니다.
     *
     * @param memberId 로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @param request  Diary 생성 요청 정보
     * @return 생성된 DiaryId
     */
    @PostMapping
    public DiaryCreateResponse createDiary(
        @Login Long memberId,
        @RequestBody DiaryCreateRequest request) {
        return diaryService.createDiary(memberId, request);
    }

    /**
     * 특정 기간의 Diary 목록을 조회합니다.
     *
     * @param memberId  로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @param startDate 조회 시작 날짜 (yyyy-MM-dd 형식)
     * @param endDate   조회 종료 날짜 (yyyy-MM-dd 형식)
     * @return 지정된 기간의 Diary 요약 정보 리스트
     */
    @GetMapping
    public DiaryMonthOverviewListResponse getMonthOverviewDiary(
        @Login Long memberId,
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    ) {
        return diaryService.getMonthOverviewDiary(memberId, startDate, endDate);
    }

    /**
     * 특정 Diary의 상세 정보를 조회합니다.
     *
     * @param memberId 로그인된 회원 ID
     * @param diaryId  조회할 Diary ID
     * @return Diary 상세 정보
     */
    @GetMapping("/{diaryId}")
    public DiaryDetailResponse getDetailDiary(@Login Long memberId, @PathVariable Long diaryId) {
        return diaryService.getDetailDiary(memberId, diaryId);
    }

    /**
     * 특정 Diary를 수정합니다.
     *
     * @param memberId 로그인된 회원 ID
     * @param diaryId  수정할 Diary ID
     * @param request  Diary 수정 요청 정보
     * @return 수정된 Diary 정보
     */
    @PatchMapping("/{diaryId}")
    public DiaryUpdateResponse updateDiary(
        @Login Long memberId,
        @PathVariable Long diaryId,
        @RequestBody DiaryUpdateRequest request) {
        return diaryService.updateDiary(memberId, diaryId, request);
    }

    /**
     * 특정 Diary를 삭제합니다.
     *
     * @param memberId 로그인된 회원 ID
     * @param diaryId  삭제할 Diary ID
     */
    @DeleteMapping("/{diaryId}")
    public void deleteDiary(@Login Long memberId, @PathVariable Long diaryId) {
        diaryService.deleteDiary(memberId, diaryId);
    }

}

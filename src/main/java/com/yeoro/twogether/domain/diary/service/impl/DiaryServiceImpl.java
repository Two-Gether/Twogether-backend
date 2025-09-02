package com.yeoro.twogether.domain.diary.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.DIARY_NOT_FOUND;
import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.diary.dto.request.DiaryCreateRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryMonthOverviewRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryUpdateRequest;
import com.yeoro.twogether.domain.diary.dto.response.DiaryCreateResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewListResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryUpdateResponse;
import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.diary.entity.Sticker;
import com.yeoro.twogether.domain.diary.repository.DiaryRepository;
import com.yeoro.twogether.domain.diary.repository.StickerRepository;
import com.yeoro.twogether.domain.diary.service.DiaryService;
import com.yeoro.twogether.domain.diary.service.mapper.DiaryMapper;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.global.exception.ServiceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final MemberService memberService;

    private final DiaryMapper diaryMapper;

    private final DiaryRepository diaryRepository;
    private final StickerRepository stickerRepository;
    private final WaypointRepository waypointRepository;
    private final WaypointItemRepository waypointItemRepository;

    @Override
    @Transactional
    public DiaryCreateResponse createDiary(Long memberId, DiaryCreateRequest request) {
        Member member = memberService.getCurrentMember(memberId);

        if (!waypointRepository.existsById(request.waypointId())) {
            throw new ServiceException(WAYPOINT_NOT_FOUND);
        }

        Diary diary = Diary.builder()
            .title(request.title())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .waypointId(request.waypointId())
            .memo(request.memo())
            .member(member)
            .build();

        diaryRepository.save(diary);

        if (request.stickerListRequest() != null
            && request.stickerListRequest().stickerRequests() != null) {
            List<Sticker> stickers = request.stickerListRequest().stickerRequests().stream()
                .map(stickerRequest -> Sticker.builder()
                    .imageUrl(stickerRequest.imageUrl())
                    .main(stickerRequest.main())
                    .diary(diary)
                    .build()
                )
                .toList();

            stickerRepository.saveAll(stickers);
        }

        return new DiaryCreateResponse(diary.getId());
    }

    @Override
    public DiaryMonthOverviewListResponse getMonthOverviewDiary(Long memberId,
        DiaryMonthOverviewRequest request) {
        Member member = memberService.getCurrentMember(memberId);

        // 해당 월의 다이어리 조회
        List<Diary> diaries = diaryRepository.findByMemberAndStartOrEndDateInMonth(
            member,
            request.startDate(),
            request.endDate()
        );

        if (diaries.isEmpty()) {
            return new DiaryMonthOverviewListResponse(Collections.emptyList());
        }

        // 각 다이어리의 main 스티커 조회
        List<Sticker> mainStickers = stickerRepository.findByDiaryInAndMainTrue(diaries);

        // diaryId -> mainStickerUrl 매핑
        Map<Long, String> diaryIdToMainStickerUrl = mainStickers.stream()
            .collect(Collectors.toMap(
                s -> s.getDiary().getId(),
                Sticker::getImageUrl
            ));

        List<DiaryMonthOverviewResponse> overviewResponses = diaries.stream()
            .map(diary -> {
                String mainStickerUrl = diaryIdToMainStickerUrl.get(diary.getId());
                return DiaryMonthOverviewResponse.builder()
                    .title(diary.getTitle())
                    .startDate(diary.getStartDate())
                    .endDate(diary.getEndDate())
                    .mainStickerUrl(mainStickerUrl)
                    .build();
            })
            .toList();

        return new DiaryMonthOverviewListResponse(overviewResponses);
    }


    @Override
    public DiaryDetailResponse getDetailDiary(Long memberId, Long diaryId) {
        Diary diary = validateAndGetDiary(memberId, diaryId);

        List<Sticker> stickers = stickerRepository.findStickersByDiary(diary);

        List<WaypointItem> topWaypointItems = new ArrayList<>();
        if (diary.getWaypointId() != null) {
            topWaypointItems = waypointItemRepository.findTop3ByWaypointIdOrderByItemOrderAsc(
                diary.getWaypointId(),
                PageRequest.of(0, 3));
        }
        return diaryMapper.toDiaryDetailResponse(diary, stickers, topWaypointItems);
    }

    @Override
    @Transactional
    public DiaryUpdateResponse updateDiary(Long memberId, Long diaryId,
        DiaryUpdateRequest request) {
        Diary diary = validateAndGetDiary(memberId, diaryId);
        if (!waypointRepository.existsById(request.waypointId())) {
            throw new ServiceException(WAYPOINT_NOT_FOUND);
        }
        diary.updateDiary(request);
        diaryRepository.save(diary);

        List<Sticker> existingStickers = stickerRepository.findStickersByDiary(diary);
        if (!existingStickers.isEmpty()) {
            stickerRepository.deleteAll(existingStickers);
        }

        List<Sticker> newStickers = request.stickerListRequest()
            .stickerRequests()
            .stream()
            .map(s -> Sticker.builder()
                .imageUrl(s.imageUrl())
                .main(s.main())
                .diary(diary)
                .build())
            .toList();

        stickerRepository.saveAll(newStickers);

        return new DiaryUpdateResponse(diary.getId());
    }

    @Override
    @Transactional
    public void deleteDiary(Long memberId, Long diaryId) {
        Diary diary = validateAndGetDiary(memberId, diaryId);
        diaryRepository.delete(diary);

        List<Sticker> stickers = stickerRepository.findStickersByDiary(diary);
        stickerRepository.deleteAll(stickers);
    }

    private Diary validateAndGetDiary(Long memberId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new ServiceException(DIARY_NOT_FOUND));
        Member member = memberService.getCurrentMember(memberId);
        diary.validateMemberOwnsDiary(member);
        return diary;
    }
}

package com.yeoro.twogether.domain.diary.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.DIARY_NOT_FOUND;
import static com.yeoro.twogether.global.exception.ErrorCode.STICKER_NOT_FOUND;

import com.yeoro.twogether.domain.diary.dto.request.DiaryCreateRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryMonthOverviewRequest;
import com.yeoro.twogether.domain.diary.dto.request.DiaryUpdateRequest;
import com.yeoro.twogether.domain.diary.dto.request.StickerRequest;
import com.yeoro.twogether.domain.diary.dto.response.DiaryCreateResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryDetailResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewListResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryMonthOverviewResponse;
import com.yeoro.twogether.domain.diary.dto.response.DiaryUpdateResponse;
import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.diary.entity.Sticker;
import com.yeoro.twogether.domain.diary.entity.StickerTemplate;
import com.yeoro.twogether.domain.diary.repository.DiaryRepository;
import com.yeoro.twogether.domain.diary.repository.StickerRepository;
import com.yeoro.twogether.domain.diary.repository.StickerTemplateRepository;
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
    private final StickerTemplateRepository stickerTemplateRepository;

    /**
     * 새로운 Diary를 생성합니다.
     * <p>
     * - 요청한 Waypoint가 존재하지 않으면 {@link ServiceException} 발생 - Diary 저장 후 Sticker 목록이 존재한다면 함께 저장
     */
    @Override
    @Transactional
    public DiaryCreateResponse createDiary(Long memberId, DiaryCreateRequest request) {
        Member member = memberService.getCurrentMember(memberId);

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
            List<Sticker> stickers = buildStickersForDiary(diary,
                request.stickerListRequest().stickerRequests());

            stickerRepository.saveAll(stickers);
        }

        return new DiaryCreateResponse(diary.getId());
    }

    /**
     * 특정 월에 해당하는 회원의 Diary 목록을 조회합니다.
     * <p>
     * - 시작일 또는 종료일이 요청 기간에 속하는 Diary를 조회 - 각 Diary에 대해 대표(main) Sticker를 매핑
     */
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
                s -> s.getTemplate().getImageUrl()  // 여기서 Template의 URL을 가져옴
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

    /**
     * 특정 Diary의 상세 정보를 조회합니다.
     * <p>
     * - Diary와 연결된 Sticker 전체를 조회 - Waypoint가 연결된 경우, 해당 Waypoint의 상위 3개 아이템을 조회
     */
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

    /**
     * 특정 Diary를 수정합니다.
     * <p>
     * - 요청한 Waypoint가 존재하지 않으면 {@link ServiceException} 발생 - 기존 Sticker 전체 삭제 후 새로 전달된 Sticker로 교체
     */
    @Override
    @Transactional
    public DiaryUpdateResponse updateDiary(Long memberId, Long diaryId,
        DiaryUpdateRequest request) {
        Diary diary = validateAndGetDiary(memberId, diaryId);

        diary.updateDiary(request);
        diaryRepository.save(diary);

        List<Sticker> existingStickers = stickerRepository.findStickersByDiary(diary);
        if (!existingStickers.isEmpty()) {
            stickerRepository.deleteAll(existingStickers);
        }

        List<Sticker> newStickers = buildStickersForDiary(diary,
            request.stickerListRequest().stickerRequests());

        stickerRepository.saveAll(newStickers);

        return new DiaryUpdateResponse(diary.getId());
    }

    /**
     * 특정 Diary를 삭제합니다.
     * <p>
     * - Diary가 존재하지 않으면 예외 발생 - Diary 삭제 시 연결된 Sticker도 함께 삭제
     */
    @Override
    @Transactional
    public void deleteDiary(Long memberId, Long diaryId) {
        Diary diary = validateAndGetDiary(memberId, diaryId);
        diaryRepository.delete(diary);

        List<Sticker> stickers = stickerRepository.findStickersByDiary(diary);
        stickerRepository.deleteAll(stickers);
    }

    private List<Sticker> buildStickersForDiary(Diary diary, List<StickerRequest> stickerRequests) {
        return stickerRequests.stream()
            .map(stickerRequest -> {
                StickerTemplate template = stickerTemplateRepository.findById(
                        stickerRequest.stickerId())
                    .orElseThrow(() -> new ServiceException(STICKER_NOT_FOUND));

                return Sticker.builder()
                    .main(stickerRequest.main())
                    .diary(diary)
                    .template(template)
                    .build();
            })
            .toList();
    }

    private Diary validateAndGetDiary(Long memberId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
            .orElseThrow(() -> new ServiceException(DIARY_NOT_FOUND));
        Member member = memberService.getCurrentMember(memberId);
        diary.validateMemberOwnsDiary(member);
        return diary;
    }
}

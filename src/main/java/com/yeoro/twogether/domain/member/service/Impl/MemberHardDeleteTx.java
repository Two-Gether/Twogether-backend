package com.yeoro.twogether.domain.member.service.Impl;

import com.yeoro.twogether.domain.diary.repository.DiaryRepository;
import com.yeoro.twogether.domain.diary.repository.StickerRepository;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.repository.MemberRepository;
import com.yeoro.twogether.domain.place.repository.PlaceRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberHardDeleteTx {

    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final DiaryRepository diaryRepository;
    private final StickerRepository stickerRepository;
    private final WaypointRepository waypointRepository;
    private final WaypointItemRepository waypointItemRepository;

    /**
     * 모든 DB 삭제를 하나의 트랜잭션으로 수행
     */
    @Transactional
    public void run(Member me,
                    List<Long> placeIds,
                    List<Long> diaryIds,
                    List<Long> waypointIds) {

        Long myId = me.getId();

        // 1) 파트너 역참조 해제 (상대/나 모두)
        memberRepository.findByPartner_Id(myId).ifPresent(other -> {
            other.connectPartner(null);
            other.clearRelationshipStartDate();
        });
        if (me.getPartner() != null) {
            me.connectPartner(null);
            me.clearRelationshipStartDate();
        }

        // 2) 자식 테이블 삭제 (FK 역순)
        // a) Sticker → Diary
        if (!diaryIds.isEmpty()) {
            stickerRepository.deleteByDiaryIds(diaryIds);
            diaryRepository.deleteByMemberId(myId);
        }

        // b) WaypointItem → Waypoint
        if (!waypointIds.isEmpty()) {
            waypointItemRepository.deleteByWaypointIds(waypointIds);
            waypointRepository.deleteByMemberId(myId);
        }

        // c) place_tags → place
        if (!placeIds.isEmpty()) {
            placeRepository.deleteTagsByPlaceIds(placeIds);
            placeRepository.deleteByMemberId(myId);
        }

        // 3) 마지막으로 member 삭제
        memberRepository.delete(me);
    }
}
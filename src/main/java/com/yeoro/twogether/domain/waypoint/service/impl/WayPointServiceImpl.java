package com.yeoro.twogether.domain.waypoint.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.waypoint.dto.WaypointItemSummaryListResponse;
import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointService;
import com.yeoro.twogether.domain.waypoint.service.mapper.WaypointItemMapper;
import com.yeoro.twogether.global.exception.ServiceException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WayPointServiceImpl implements WaypointService {

    private final MemberService memberService;
    private final WaypointRepository waypointRepository;
    private final WaypointItemRepository waypointItemRepository;
    private final WaypointItemMapper waypointItemMapper;

    /**
     * <p>name & memberId를 통한 member 기반 waypoint 생성</p>
     * <p>저장 후 생성된 waypointId 반환</p>
     */
    @Override
    @Transactional
    public Long createWaypoint(Long memberId, String name) {
        Member member = memberService.getMemberByMemberId(memberId);
        Waypoint waypoint = Waypoint.builder()
            .name(name)
            .member(member)
            .build();
        waypointRepository.save(waypoint);
        return waypoint.getId();
    }

    /**
     * <p>waypointId 기반으로 waypoint name, waypointItem name & imageUrl (SummaryList) 조회</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public WaypointItemSummaryListResponse getWaypointSummaryList(Long memberId, Long waypointId) {
        Waypoint waypoint = validateAndGetWaypoint(memberId, waypointId);
        List<WaypointItem> waypointItems = waypointItemRepository.findWaypointItemsByWaypointId(waypointId);
        return waypointItemMapper.toWaypointItemSummaryListResponse(waypoint.getName(), waypointItems);
    }

    /**
     * <p>수정할 name을 통해 waypoint 정보 수정</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    @Transactional
    public Long updateWaypoint(Long memberId, Long waypointId, String name) {
        Waypoint waypoint = validateAndGetWaypoint(memberId, waypointId);
        waypoint.updateWaypoint(name);
        waypointRepository.save(waypoint);
        return waypoint.getId();
    }

    /**
     * <p>waypointId 기반으로 waypoint 삭제</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    @Transactional
    public void deleteWaypoint(Long memberId, Long waypointId) {
        Waypoint waypoint = validateAndGetWaypoint(memberId, waypointId);
        waypointItemRepository.deleteByWaypointId(waypointId);
        waypointRepository.delete(waypoint);
    }

    private Waypoint validateAndGetWaypoint(Long memberId, Long waypointId) {
        Waypoint waypoint = waypointRepository.findById(waypointId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        Member member = memberService.getMemberByMemberId(memberId);
        waypoint.validateMemberOwnsWaypoint(member);
        return waypoint;
    }
}

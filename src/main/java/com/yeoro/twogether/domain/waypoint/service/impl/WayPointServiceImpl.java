package com.yeoro.twogether.domain.waypoint.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointCreateRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointUpdateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointWithItemsResponse;
import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointService;
import com.yeoro.twogether.domain.waypoint.service.mapper.WaypointMapper;
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
    private final WaypointMapper waypointMapper;

    /**
     * <p>name & memberId를 통한 member 기반 waypoint 생성</p>
     * <p>저장 후 생성된 waypointId 반환</p>
     */
    @Override
    @Transactional
    public WaypointCreateResponse createWaypoint(Long memberId, WaypointCreateRequest request) {
        Member member = memberService.getMemberByMemberId(memberId);
        Waypoint waypoint = Waypoint.builder()
            .name(request.name())
            .member(member)
            .build();
        waypointRepository.save(waypoint);
        return new WaypointCreateResponse(waypoint.getId());
    }

    /**
     * <p>waypointId 기반으로 waypoint name, waypointItem name & imageUrl (SummaryList) 조회</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public WaypointWithItemsResponse getWaypoint(Long memberId, Long waypointId) {
        Waypoint waypoint = validateAndGetWaypoint(memberId, waypointId);
        List<WaypointItem> waypointItems = waypointItemRepository.findWaypointItemsByWaypointId(
            waypointId);
        return waypointMapper.toWaypointWithItemsResponse(waypoint.getName(), waypointItems);
    }

    /**
     * <p>수정할 name을 통해 waypoint 정보 수정</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    @Transactional
    public WaypointUpdateResponse updateWaypoint(Long memberId, Long waypointId,
        WaypointUpdateRequest request) {
        Waypoint waypoint = validateAndGetWaypoint(memberId, waypointId);
        waypoint.updateWaypoint(request.name());
        waypointRepository.save(waypoint);
        return new WaypointUpdateResponse(waypoint.getId());
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

    /**
     * memberId를 기반으로 Waypoint 검증 후 반환
     */
    private Waypoint validateAndGetWaypoint(Long memberId, Long waypointId) {
        Waypoint waypoint = waypointRepository.findById(waypointId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        Member member = memberService.getMemberByMemberId(memberId);
        waypoint.validateMemberOwnsWaypoint(member);
        return waypoint;
    }
}

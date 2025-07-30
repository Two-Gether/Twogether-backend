package com.yeoro.twogether.domain.waypoint.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointItemService;
import com.yeoro.twogether.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WaypointItemServiceImpl implements WaypointItemService {

    private final MemberService memberService;

    private final WaypointItemRepository waypointItemRepository;
    private final WaypointRepository waypointRepository;

    /**
     * <p>Long waypointId, String name, String address, String imageUrl 기반으로 waypointItem 생성</p>
     * <p>저장 후 생성된 waypointItemId 반환</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public WaypointItemCreateResponse addWaypointItem(Long memberId, Long waypointId,
        WaypointItemAddRequest request) {
        Member member = memberService.getCurrentMember(memberId);
        Waypoint waypoint = waypointRepository.findById(waypointId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        waypoint.validateMemberOwnsWaypoint(member);

        Long maxOrder = waypointItemRepository.findMaxOrderByWaypointId(waypointId);
        Long nextOrder = maxOrder + 1;

        WaypointItem waypointItem = WaypointItem.builder()
            .name(request.name())
            .address(request.address())
            .imageUrl(request.imageUrl())
            .waypoint(waypoint)
            .itemOrder(nextOrder)
            .build();
        waypointItemRepository.save(waypointItem);

        return new WaypointItemCreateResponse(waypointItem.getId(), waypointItem.getItemOrder());
    }

    /**
     * <p>waypointItemId 기반으로 waypointItem 삭제</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public void deleteWaypointItem(Long memberId, Long waypointId, Long waypointItemId) {
        Member member = memberService.getCurrentMember(memberId);
        WaypointItem waypointItem = waypointItemRepository.findById(waypointItemId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));

        waypointItem.validateBelongsTo(waypointId);
        waypointItem.validateOwnedBy(member);

        waypointItemRepository.delete(waypointItem);
    }
}

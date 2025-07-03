package com.yeoro.twogether.domain.waypoint.service.impl;

import com.yeoro.twogether.domain.waypoint.dto.WaypointItemSummaryListResponse;
import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointService;
import com.yeoro.twogether.domain.waypoint.service.mapper.WaypointItemMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WayPointServiceImpl implements WaypointService {

    private final WaypointRepository waypointRepository;
    private final WaypointItemRepository waypointItemRepository;

    private final WaypointItemMapper waypointItemMapper;

    /**
     * <p>넘어온 name으로 waypoint 생성</p>
     * <p>저장 후 생성된 waypointId 반환</p>
     * member controller 구현 완료 후 waypoint에 추가 ( 관련 Service도 변경 예정 , member 관련 검증들 아직 미구현 )
     */
    @Override
    @Transactional
    public Long createWaypoint(String name) {
        Waypoint waypoint = Waypoint.builder()
            .name(name)
//          .member(new Member() ...)
            .build();
        waypointRepository.save(waypoint);
        return waypoint.getId();
    }

    /**
     * <p>waypointId 기반으로 waypoint name, waypointItem name & imageUrl (SummaryList) 조회</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public WaypointItemSummaryListResponse getWaypointSummaryList(Long waypointId) {
        Waypoint waypoint = waypointRepository.findById(waypointId).orElseThrow(null);
        List<WaypointItem> waypointItems = waypointItemRepository.findWaypointItemsByWaypointId(waypointId);
        return waypointItemMapper.toWaypointItemSummaryListResponse(waypoint.getName(), waypointItems);
    }

    /**
     * <p>수정할 name을 통해 waypoint 정보 수정</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    @Transactional
    public Long updateWaypoint(Long waypointId, String name) {
        Waypoint waypoint = waypointRepository.findById(waypointId).orElseThrow(null);
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
    public void deleteWaypoint(Long waypointId) {
        Waypoint waypoint = waypointRepository.findById(waypointId).orElseThrow(null);
        waypointItemRepository.deleteByWaypointId(waypointId);
        waypointRepository.delete(waypoint);
    }
}

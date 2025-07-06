package com.yeoro.twogether.domain.waypoint.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointItemService;
import com.yeoro.twogether.global.exception.ErrorCode;
import com.yeoro.twogether.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WaypointItemServiceImpl implements WaypointItemService {

    private final WaypointItemRepository waypointItemRepository;
    private final WaypointRepository waypointRepository;

    /**
     * <p>Long waypointId, String name, String address, String imageUrl 기반으로 waypointItem 생성</p>
     * <p>저장 후 생성된 waypointItemId 반환</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public Long addWaypointItem(Long waypointId, String name, String address, String imageUrl) {
        Waypoint waypoint = waypointRepository.findById(waypointId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        WaypointItem waypointItem = WaypointItem.builder()
            .name(name)
            .address(address)
            .imageUrl(imageUrl)
            .waypoint(waypoint)
            .build();
        waypointItemRepository.save(waypointItem);
        return waypointItem.getId();
    }

    /**
     * <p>waypointItemId 기반으로 waypointItem 삭제</p>
     * 존재하지 않으면 ServiceException 발생
     */
    @Override
    public void deleteWaypointItem(Long waypointItemId) {
        WaypointItem waypointItem = waypointItemRepository.findById(waypointItemId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        waypointItemRepository.delete(waypointItem);
    }
}

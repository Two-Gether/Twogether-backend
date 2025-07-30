package com.yeoro.twogether.domain.waypoint.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_ITEM_NOT_MATCHED;
import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_ITEM_ORDER_INVALID;
import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemDeleteRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemReorderRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointItemService;
import com.yeoro.twogether.global.exception.ServiceException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
     * waypointId와 회원 ID를 기준으로 소유권 검증 후, WaypointItem 생성 요청 정보(name, address, imageUrl)로 새로운
     * WaypointItem을 생성하고 저장함. 현재 waypoint에 등록된 최대 순서값을 조회해, 그 다음 순서로 설정. 저장 후 생성된 WaypointItem의 ID와
     * 순서를 반환.
     */
    @Override
    public WaypointItemCreateResponse addWaypointItem(Long memberId, Long waypointId,
        WaypointItemAddRequest request) {
        Waypoint waypoint = getOwnedWaypoint(memberId, waypointId);

        int maxOrder = waypointItemRepository.findMaxOrderByWaypointId(waypointId);
        int nextOrder = maxOrder + 1;

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
     * waypointId와 회원 ID를 기준으로 소유권 검증 후, 요청받은 orderedIds 순서대로 WaypointItem의 순서를 업데이트함. orderedIds가
     * 비었거나 null이면 예외를 던짐. orderedIds에 포함된 모든 ID가 실제 데이터에 존재하는지 확인하고, waypointId에 속한 항목인지 검증함. 검증이
     * 완료되면 각 항목의 순서를 orderedIds 리스트의 인덱스+1 값으로 갱신함.
     */
    @Override
    public void reorderWaypointItem(Long memberId, Long waypointId,
        WaypointItemReorderRequest request) {
        getOwnedWaypoint(memberId, waypointId);

        List<Long> orderedIds = request.orderedIds();
        validateOrderedIds(orderedIds);

        List<WaypointItem> items = waypointItemRepository.findAllById(orderedIds);
        validateItems(orderedIds, items, waypointId);

        Map<Long, WaypointItem> itemMap = items.stream()
            .collect(Collectors.toMap(WaypointItem::getId, item -> item));

        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            WaypointItem item = itemMap.get(id);
            item.updateOrder(i + 1);
        }

        waypointItemRepository.saveAll(items);
    }

    /**
     * waypointItemIds로 다수의 WaypointItem을 삭제함. 각 항목에 대해 waypoint 소속 여부와 member 소유권을 검증함. 삭제 후, 해당
     * waypoint의 남아 있는 항목들을 순서대로 다시 itemOrder 재정렬.
     */
    @Override
    public void deleteWaypointItems(Long memberId, Long waypointId,
        WaypointItemDeleteRequest request) {
        Member member = memberService.getCurrentMember(memberId);

        List<Long> waypointItemIds = request.waypointItemIds();
        validateWaypointItemIds(waypointItemIds);

        List<WaypointItem> itemsToDelete = waypointItemRepository.findAllById(waypointItemIds);

        if (itemsToDelete.size() != waypointItemIds.size()) {
            throw new ServiceException(WAYPOINT_ITEM_NOT_MATCHED);
        }

        validateOwnership(itemsToDelete, waypointId, member);

        waypointItemRepository.deleteAll(itemsToDelete);

        Waypoint waypoint = itemsToDelete.get(0).getWaypoint();

        reorderRemainingItems(waypoint);
    }

    private Waypoint getOwnedWaypoint(Long memberId, Long waypointId) {
        Member member = memberService.getCurrentMember(memberId);
        Waypoint waypoint = waypointRepository.findById(waypointId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        waypoint.validateMemberOwnsWaypoint(member);
        return waypoint;
    }

    // === [Validation Methods] ===

    private void validateWaypointItemIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException(WAYPOINT_ITEM_ORDER_INVALID);
        }
    }

    private void validateOwnership(List<WaypointItem> items, Long waypointId, Member member) {
        for (WaypointItem item : items) {
            item.validateBelongsTo(waypointId);
            item.validateOwnedBy(member);
        }
    }

    private void validateOrderedIds(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new ServiceException(WAYPOINT_ITEM_ORDER_INVALID);
        }
    }

    private void validateItems(List<Long> orderedIds, List<WaypointItem> items, Long waypointId) {
        if (items.size() != orderedIds.size()) {
            throw new ServiceException(WAYPOINT_ITEM_NOT_MATCHED);
        }

        items.forEach(item -> {
            if (!item.getWaypoint().getId().equals(waypointId)) {
                throw new ServiceException(WAYPOINT_ITEM_NOT_MATCHED);
            }
        });
    }

    private void reorderRemainingItems(Waypoint waypoint) {
        List<WaypointItem> remainingItems = waypointItemRepository.findByWaypointOrderByItemOrderAsc(
            waypoint);

        for (int i = 0; i < remainingItems.size(); i++) {
            remainingItems.get(i).updateOrder(i + 1);
        }

        waypointItemRepository.saveAll(remainingItems);
    }

}

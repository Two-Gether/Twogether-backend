package com.yeoro.twogether.domain.waypoint.service.impl;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_ITEM_NOT_MATCHED;
import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_ITEM_ORDER_INVALID;
import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_NOT_FOUND;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.domain.member.service.MemberService;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemCopyRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemDeleteRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemReorderRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemUpdateResponse;
import com.yeoro.twogether.domain.waypoint.entity.Waypoint;
import com.yeoro.twogether.domain.waypoint.entity.WaypointItem;
import com.yeoro.twogether.domain.waypoint.repository.WaypointItemRepository;
import com.yeoro.twogether.domain.waypoint.repository.WaypointRepository;
import com.yeoro.twogether.domain.waypoint.service.WaypointItemService;
import com.yeoro.twogether.global.exception.ServiceException;
import java.util.ArrayList;
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
     * <p>지정된 Waypoint에 새 WaypointItem을 추가합니다.</p>
     *
     * <p>회원 ID와 Waypoint ID를 기준으로 소유권을 검증하고,
     * 요청된 정보(name, address, imageUrl, memo)를 바탕으로 새 WaypointItem을 생성합니다. 현재 Waypoint에 등록된 최대
     * itemOrder를 조회하여 다음 순서를 지정한 후 저장하고, 생성된 항목의 ID와 순서를 응답합니다.</p>
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
            .memo(request.memo())
            .waypoint(waypoint)
            .itemOrder(nextOrder)
            .build();
        waypointItemRepository.save(waypointItem);

        return new WaypointItemCreateResponse(waypointItem.getId(), waypointItem.getItemOrder());
    }

    /**
     * <p>WaypointItem들의 순서를 재정렬합니다.</p>
     *
     * <p>Waypoint ID와 회원 ID 기준으로 소유권을 확인한 후,
     * 요청된 ID 순서대로 각 WaypointItem의 itemOrder를 업데이트합니다. 순서가 유효하지 않거나, 요청된 항목이 존재하지 않거나 다른 Waypoint에
     * 속할 경우 예외를 던집니다.</p>
     */
    @Override
    public void reorderWaypointItem(Long memberId, Long waypointId,
        WaypointItemReorderRequest request) {

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
     * <p>특정 WaypointItem의 정보를 수정합니다.</p>
     *
     * <p>Waypoint ID와 회원 ID를 기준으로 해당 항목이 주어진 Waypoint에 속하고,
     * 해당 회원이 소유자인지를 검증한 뒤, 요청된 메모 등의 정보를 반영하여 수정합니다.</p>
     */
    @Override
    public WaypointItemUpdateResponse updateWaypointItem(Long memberId, Long waypointId,
        Long waypointItemId, WaypointItemUpdateRequest request) {

        Member member = memberService.getCurrentMember(memberId);

        WaypointItem waypointItem = waypointItemRepository.findById(waypointItemId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));

        waypointItem.validateBelongsTo(waypointId);
        waypointItem.validateOwnedBy(member);

        waypointItem.update(request);
        waypointItemRepository.save(waypointItem);

        return new WaypointItemUpdateResponse(waypointItem.getMemo());
    }

    /**
     * <p>WaypointItem들을 복사하여 다른 Waypoint에 붙여넣습니다.</p>
     *
     * <p>복사 대상이 되는 WaypointItem ID 리스트가 요청에 포함되어 있으며,
     * 이 항목들이 실제 존재하고, source Waypoint에 속하며, 해당 멤버가 소유하고 있는지 확인합니다. 그런 다음 targetWaypoint에 대해 소유권 검사를
     * 거친 후, 해당 WaypointItem들을 복제하여 targetWaypoint에 저장합니다. 순서는 기존 항목들의 maxOrder 이후부터 지정됩니다.</p>
     */
    @Override
    public void copyWaypointItems(Long memberId, Long waypointId, Long targetWaypointId,
        WaypointItemCopyRequest request) {
        Member member = memberService.getCurrentMember(memberId);

        List<Long> waypointItemIds = request.waypointItemIds();
        List<WaypointItem> sourceItems = getValidatedItemsByIds(waypointItemIds, waypointId,
            member);

        Waypoint targetWaypoint = getOwnedWaypoint(memberId, targetWaypointId);

        List<WaypointItem> copiedItems = new ArrayList<>();
        int startOrder = waypointItemRepository.findMaxOrderByWaypointId(targetWaypointId) + 1;

        for (WaypointItem item : sourceItems) {
            WaypointItem copied = WaypointItem.builder()
                .name(item.getName())
                .address(item.getAddress())
                .imageUrl(item.getImageUrl())
                .memo(item.getMemo())
                .waypoint(targetWaypoint)
                .itemOrder(startOrder++)
                .build();

            copiedItems.add(copied);
        }
        waypointItemRepository.saveAll(copiedItems);
    }

    /**
     * <p>다수의 WaypointItem을 삭제합니다.</p>
     *
     * <p>요청된 ID 리스트에 포함된 각 항목이 실제로 존재하는지,
     * 요청한 Waypoint에 속해 있는지, 요청자의 소유 항목인지 검증한 후 삭제합니다. 이후 남아 있는 항목들의 itemOrder를 1부터 다시 정렬합니다.</p>
     */
    @Override
    public void deleteWaypointItems(Long memberId, Long waypointId,
        WaypointItemDeleteRequest request) {
        Member member = memberService.getCurrentMember(memberId);

        List<Long> waypointItemIds = request.waypointItemIds();
        List<WaypointItem> itemsToDelete = getValidatedItemsByIds(waypointItemIds, waypointId,
            member);

        waypointItemRepository.deleteAll(itemsToDelete);
        reorderRemainingItems(itemsToDelete.get(0).getWaypoint());
    }

    private List<WaypointItem> getValidatedItemsByIds(List<Long> waypointItemIds, Long waypointId,
        Member member) {
        validateWaypointItemIds(waypointItemIds);

        List<WaypointItem> items = waypointItemRepository.findAllById(waypointItemIds);
        if (items.size() != waypointItemIds.size()) {
            throw new ServiceException(WAYPOINT_ITEM_NOT_MATCHED);
        }

        validateOwnership(items, waypointId, member);
        return items;
    }

    private Waypoint getOwnedWaypoint(Long memberId, Long waypointId) {
        Member member = memberService.getCurrentMember(memberId);
        Waypoint waypoint = waypointRepository.findById(waypointId)
            .orElseThrow(() -> new ServiceException(WAYPOINT_NOT_FOUND));
        waypoint.validateMemberOwnsWaypoint(member);
        return waypoint;
    }

    private void reorderRemainingItems(Waypoint waypoint) {
        List<WaypointItem> remainingItems = waypointItemRepository.findByWaypointOrderByItemOrderAsc(
            waypoint);

        for (int i = 0; i < remainingItems.size(); i++) {
            remainingItems.get(i).updateOrder(i + 1);
        }

        waypointItemRepository.saveAll(remainingItems);
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

}

package com.yeoro.twogether.domain.waypoint.controller;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemDeleteRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemReorderRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemUpdateResponse;
import com.yeoro.twogether.domain.waypoint.service.WaypointItemService;
import com.yeoro.twogether.global.argumentResolver.Login;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/waypoint/{waypointId}/items")
public class WaypointItemController {

    private final WaypointItemService waypointItemService;

    /**
     * 특정 Waypoint에 새로운 WaypointItem을 추가합니다.
     *
     * @param memberId               로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @param waypointId             Waypoint ID (추가할 대상)
     * @param waypointItemAddRequest WaypointItem 생성 요청 정보
     * @return 생성된 WaypointItemId
     */
    @PostMapping
    public WaypointItemCreateResponse addWaypointItem(@Login Long memberId,
        @PathVariable Long waypointId, @RequestBody WaypointItemAddRequest waypointItemAddRequest) {
        return waypointItemService.addWaypointItem(memberId, waypointId, waypointItemAddRequest);
    }

    /**
     * 특정 Waypoint에 속한 WaypointItem의 정보를 수정합니다.
     *
     * @param memberId                  로그인된 회원 ID (@Login 커스텀 리졸버 사용)
     * @param waypointId                Waypoint ID (해당 항목이 속한 경유지 ID)
     * @param waypointItemId            수정할 WaypointItem ID
     * @param waypointItemUpdateRequest 수정 요청 정보 (예: 메모 등)
     * @return 수정된 WaypointItem 정보
     */
    @PatchMapping("/{waypointItemId}")
    public WaypointItemUpdateResponse updateWaypointItem(@Login Long memberId,
        @PathVariable Long waypointId,
        @PathVariable Long waypointItemId,
        @RequestBody WaypointItemUpdateRequest waypointItemUpdateRequest) {
        return waypointItemService.updateWaypointItem(memberId, waypointId, waypointItemId,
            waypointItemUpdateRequest);
    }


    /**
     * 특정 Waypoint에 속한 WaypointItem들의 순서를 재정렬합니다.
     *
     * @param memberId                   로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @param waypointId                 Waypoint ID
     * @param waypointItemReorderRequest 재정렬할 WaypointItem ID 리스트를 포함한 요청 객체
     */
    @PatchMapping
    public void reorderWaypointItems(@Login Long memberId,
        @PathVariable Long waypointId,
        @RequestBody WaypointItemReorderRequest waypointItemReorderRequest) {
        waypointItemService.reorderWaypointItem(memberId, waypointId, waypointItemReorderRequest);
    }

    /**
     * 특정 Waypoint에 속한 여러 개의 WaypointItem을 삭제합니다.
     * <p>
     * 요청 본문에 포함된 WaypointItem ID 리스트에 대해 소유권 및 waypoint 소속 여부를 검증한 뒤, 해당 항목들을 삭제하고 나머지 항목들의 순서를
     * 재정렬합니다.
     *
     * @param memberId                  로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @param waypointId                삭제 대상이 속한 Waypoint ID
     * @param waypointItemDeleteRequest 삭제할 WaypointItem ID 목록을 담은 요청 객체
     */
    @DeleteMapping
    public void deleteWaypointItems(@Login Long memberId, @PathVariable Long waypointId,
        @RequestBody WaypointItemDeleteRequest waypointItemDeleteRequest) {
        waypointItemService.deleteWaypointItems(memberId, waypointId, waypointItemDeleteRequest);
    }
}
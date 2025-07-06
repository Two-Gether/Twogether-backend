package com.yeoro.twogether.domain.waypoint.controller;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointItemAddRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointItemCreateResponse;
import com.yeoro.twogether.domain.waypoint.service.WaypointItemService;
import com.yeoro.twogether.global.argumentResolver.Login;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
     * 특정 Waypoint에 속한 WaypointItem을 삭제합니다.
     *
     * @param memberId       로그인된 회원 ID
     * @param waypointId     Waypoint ID
     * @param waypointItemId 삭제할 WaypointItem ID
     */
    @DeleteMapping("/{waypointItemId}")
    public void deleteWaypointItem(@Login Long memberId, @PathVariable Long waypointId,
        @PathVariable Long waypointItemId) {
        waypointItemService.deleteWaypointItem(memberId, waypointId, waypointItemId);
    }
}
package com.yeoro.twogether.domain.waypoint.controller;

import com.yeoro.twogether.domain.waypoint.dto.request.WaypointCreateRequest;
import com.yeoro.twogether.domain.waypoint.dto.request.WaypointUpdateRequest;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointCreateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointSummaryListResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointUpdateResponse;
import com.yeoro.twogether.domain.waypoint.dto.response.WaypointWithItemsResponse;
import com.yeoro.twogether.domain.waypoint.service.WaypointService;
import com.yeoro.twogether.global.argumentResolver.Login;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Waypoint 관련 CRUD 요청을 처리하는 REST 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/waypoint")
public class WaypointController {

    private final WaypointService waypointService;

    /**
     * 새로운 Waypoint를 생성합니다.
     *
     * @param memberId 로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @param request  Waypoint 생성 요청 정보
     * @return 생성된 WaypointId
     */
    @PostMapping
    public WaypointCreateResponse createWaypoint(@Login Long memberId,
        @RequestBody WaypointCreateRequest request) {
        return waypointService.createWaypoint(memberId, request);
    }

    /**
     * 회원이 소유한 모든 Waypoint 목록을 조회합니다.
     *
     * @param memberId 로그인된 회원 ID (커스텀 리졸버 @Login 이용)
     * @return Waypoint 요약 정보 리스트
     */
    @GetMapping
    public WaypointSummaryListResponse getAllWaypoints(@Login Long memberId) {
        return waypointService.getAllWaypoints(memberId);
    }

    /**
     * 특정 Waypoint와 그에 속한 WaypointItem 목록을 조회합니다.
     *
     * @param memberId   로그인된 회원 ID
     * @param waypointId 조회할 Waypoint ID
     * @return Waypoint 정보 및 그에 속한 아이템 목록
     */
    @GetMapping("/{waypointId}")
    public WaypointWithItemsResponse getWaypoint(@Login Long memberId,
        @PathVariable Long waypointId) {
        return waypointService.getWaypoint(memberId, waypointId);
    }

    /**
     * 특정 Waypoint의 이름을 수정합니다.
     *
     * @param memberId   로그인된 회원 ID
     * @param waypointId 수정할 Waypoint ID
     * @param request    Waypoint 수정 요청 정보
     * @return 수정된 WaypointId
     */
    @PatchMapping("/{waypointId}")
    public WaypointUpdateResponse updateWaypoint(@Login Long memberId,
        @PathVariable Long waypointId, @RequestBody WaypointUpdateRequest request) {
        return waypointService.updateWaypoint(memberId, waypointId, request);
    }

    /**
     * 특정 Waypoint를 삭제합니다.
     *
     * @param memberId   로그인된 회원 ID
     * @param waypointId 삭제할 Waypoint ID
     */
    @DeleteMapping("/{waypointId}")
    public void deleteWaypoint(@Login Long memberId, @PathVariable Long waypointId) {
        waypointService.deleteWaypoint(memberId, waypointId);
    }
}

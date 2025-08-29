package com.yeoro.twogether.domain.place.controller;

import com.yeoro.twogether.domain.place.dto.request.PlaceCreateRequest;
import com.yeoro.twogether.domain.place.dto.request.PlaceUpdateRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceCreateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceOneSearchRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceResponse;
import com.yeoro.twogether.domain.place.service.PlaceService;
import com.yeoro.twogether.global.argumentResolver.Login;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Place(highlights) 관련 CRUD를 처리하는 REST 컨트롤러입니다.
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place")
public class PlaceController {
    private final PlaceService placeService;


    /**
     * 하이라이트를 생성합니다.
     * 동일 유저 기준 하나만 생성할 수 있습니다.
     * @param memberId 로그인한 유저
     * @param request 생성 요청 DTO
     * @return 저장한 Place(하이라이트) 객체
     */
    @PostMapping
    public PlaceCreateResponse createPlace(@Login Long memberId,
                                           @RequestBody PlaceCreateRequest request) {
        return placeService.createPlace(memberId, request);
    }

    /**
     * 주소로 Place 목록 조회
     */
    @GetMapping
    public List<PlaceResponse> getPlace(@Login Long memberId, @RequestParam String address) {
        return placeService.getPlace(address);
    }

    @GetMapping("/one")
    public PlaceResponse getOnePlace(@Login Long memberId,
                                          @RequestParam String address) {
        return placeService.getOnePlace(memberId, new PlaceOneSearchRequest(address));
    }

    @PutMapping
    public PlaceResponse updatePlace(@Login Long memberId,
                                     @RequestBody PlaceUpdateRequest request) {
        return placeService.updatePlace(memberId, request);
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<String> deletePlace(@Login Long memberId,
                                            @PathVariable Long placeId) {
        placeService.deletePlace(memberId, placeId);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}

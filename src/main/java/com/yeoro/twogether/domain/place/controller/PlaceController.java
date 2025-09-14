package com.yeoro.twogether.domain.place.controller;

import com.yeoro.twogether.domain.place.dto.response.PlaceCreateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceResponse;
import com.yeoro.twogether.domain.place.service.PlaceService;
import com.yeoro.twogether.global.argumentResolver.Login;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Place(highlights) 관련 CRUD를 처리하는 REST 컨트롤러입니다.
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place")
public class PlaceController {
    private final PlaceService placeService;


    /** 하이라이트를 생성합니다. (동일 유저 기준 하나만 생성)*/
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlaceCreateResponse createPlace(
            @Login Long memberId,
            @RequestPart("meta") String metaJson,
            @RequestPart("image") MultipartFile image
    ) {
        return placeService.createPlace(memberId, metaJson, image);
    }

    /**주소로 Place 목록 조회*/
    @GetMapping
    public List<PlaceResponse> getPlace(@Login Long memberId, @RequestParam String address) {
        return placeService.getPlace(address);
    }

    /**주소로 Place 단건 조회*/
    @GetMapping("/{placeId}")
    public PlaceResponse getOnePlace(@Login Long memberId,
                                     @PathVariable Long placeId) {
        return placeService.getOnePlace(memberId, placeId);
    }

    /** 하이라이트 수정*/
    @PutMapping(value = "/{placeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlaceResponse updatePlace(
            @Login Long memberId,
            @PathVariable Long placeId,
            @RequestPart("meta") String metaJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return placeService.updatePlace(memberId, placeId, metaJson, image);
    }


    @DeleteMapping("/{placeId}")
    public ResponseEntity<String> deletePlace(@Login Long memberId,
                                            @PathVariable Long placeId) {
        placeService.deletePlace(memberId, placeId);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}

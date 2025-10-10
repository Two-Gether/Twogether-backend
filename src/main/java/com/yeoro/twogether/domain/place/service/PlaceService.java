package com.yeoro.twogether.domain.place.service;

import com.yeoro.twogether.domain.place.dto.response.PlaceByDateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceCreateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PlaceService {

    PlaceCreateResponse createPlace(Long memberId, String metaJson, MultipartFile image);
    List<PlaceResponse> getPlace(String address);
    void deletePlace(Long memberId, Long placeId);
    PlaceResponse getOnePlace(Long memberId, Long placeId);
    PlaceResponse updatePlace(Long memberId, Long placeId, String metaJson, MultipartFile image);
    PlaceByDateResponse getPlacesByDate(Long memberId, LocalDate dateKst);
}
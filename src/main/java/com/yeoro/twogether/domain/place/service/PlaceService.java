package com.yeoro.twogether.domain.place.service;

import com.yeoro.twogether.domain.place.dto.request.PlaceCreateRequest;
import com.yeoro.twogether.domain.place.dto.request.PlaceUpdateRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceCreateResponse;
import com.yeoro.twogether.domain.place.dto.response.PlaceOneSearchRequest;
import com.yeoro.twogether.domain.place.dto.response.PlaceResponse;

import java.util.List;

public interface PlaceService {

    PlaceCreateResponse createPlace(Long memberId, PlaceCreateRequest request);
    List<PlaceResponse> getPlace(String address);
    void deletePlace(Long memberId, Long placeId);
    PlaceResponse getOnePlace(Long memberId, PlaceOneSearchRequest request);
    PlaceResponse updatePlace(Long memberId, PlaceUpdateRequest request);
}
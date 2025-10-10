package com.yeoro.twogether.domain.place.dto.response;

import java.util.List;

public record PlaceByDateResponse(
        List<PlaceResponse> mine,
        List<PlaceResponse> partner
) {}
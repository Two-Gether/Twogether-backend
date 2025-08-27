package com.yeoro.twogether.domain.place.dto.request;

import java.util.List;

public record PlaceUpdateRequest(
        String address,       // 수정 대상 식별 키
        String imageUrl,
        String name,
        String description,
        List<String> tags
) { }

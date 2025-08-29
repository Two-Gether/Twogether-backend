package com.yeoro.twogether.domain.place.dto.request;

import java.util.List;

public record PlaceCreateRequest (
        String imageUrl,
        String name,
        String address,
        String description,
        List<String> tags
) {}

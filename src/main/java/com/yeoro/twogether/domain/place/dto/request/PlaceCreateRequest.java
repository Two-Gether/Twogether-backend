package com.yeoro.twogether.domain.place.dto.request;

import java.util.List;

public record PlaceCreateRequest(
        String name,
        String address,
        String description,
        List<String> tags
) {}
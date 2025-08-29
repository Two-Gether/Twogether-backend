package com.yeoro.twogether.domain.place.dto.response;

import com.yeoro.twogether.domain.place.entity.Place;

import java.util.ArrayList;
import java.util.List;

public record PlaceCreateResponse(
        Long id,
        String imageUrl,
        String name,
        String address,
        String description,
        List<String> tags
) {
    public static PlaceCreateResponse from(Place place) {
        List<String> safeTags = (place.getTags() == null) ? List.of() : new ArrayList<>(place.getTags());
        return new PlaceCreateResponse(
                place.getId(),
                place.getImageUrl(),
                place.getName(),
                place.getAddress(),
                place.getDescription(),
                safeTags
        );
    }
}
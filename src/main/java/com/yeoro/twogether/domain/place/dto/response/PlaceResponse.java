package com.yeoro.twogether.domain.place.dto.response;

import com.yeoro.twogether.domain.place.entity.Place;

import java.util.ArrayList;
import java.util.List;

public record PlaceResponse(
        Long id,
        Long memberId,
        String imageUrl,
        String name,
        String address,
        String description,
        List<String> tags
) {
    public static PlaceResponse from(Place p) {
        List<String> safeTags = (p.getTags() == null) ? List.of() : new ArrayList<>(p.getTags());
        return new PlaceResponse(
                p.getId(),
                p.getMember() != null ? p.getMember().getId() : null,
                p.getImageUrl(),
                p.getName(),
                p.getAddress(),
                p.getDescription(),
                safeTags
        );
    }

    public static List<PlaceResponse> fromList(List<Place> list) {
        return list.stream().map(PlaceResponse::from).toList();
    }
}
package com.yeoro.twogether.domain.place.dto.response;

import com.yeoro.twogether.domain.place.entity.Place;

import java.util.List;

public record PlaceCreateResponse(
        Long id,
        String imageUrl,   // 응답에는 presigned URL이 올 것
        String name,
        String address,
        String description,
        List<String> tags  // 불변 리스트로 방어적 복사
) {
    public static PlaceCreateResponse from(Place place) {
        List<String> safeTags = (place.getTags() == null)
                ? List.of()
                : List.copyOf(place.getTags());

        return new PlaceCreateResponse(
                place.getId(),
                place.getImageUrl(),
                place.getName(),
                place.getAddress(),
                place.getDescription(),
                safeTags
        );
    }

    public static PlaceCreateResponse fromWithResolvedUrl(Place place, String presignedUrl) {
        List<String> safeTags = (place.getTags() == null)
                ? List.of()
                : List.copyOf(place.getTags());

        return new PlaceCreateResponse(
                place.getId(),
                presignedUrl, // 프리사인드 URL
                place.getName(),
                place.getAddress(),
                place.getDescription(),
                safeTags
        );
    }
}

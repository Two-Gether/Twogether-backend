package com.yeoro.twogether.domain.place.dto.response;

import com.yeoro.twogether.domain.place.entity.Place;

import java.util.List;

public record PlaceResponse(
        Long id,
        Long memberId,
        String imageUrl,   // 응답 시: presigned URL
        String name,
        String address,
        String description,
        List<String> tags
) {
    public static PlaceResponse from(Place p) {
        List<String> safeTags = (p.getTags() == null) ? List.of() : List.copyOf(p.getTags());
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

    public static PlaceResponse fromWithResolvedUrl(Place p, String presignedUrl) {
        List<String> safeTags = (p.getTags() == null) ? List.of() : List.copyOf(p.getTags());
        return new PlaceResponse(
                p.getId(),
                p.getMember() != null ? p.getMember().getId() : null,
                presignedUrl, // ← 클라이언트가 바로 접근 가능한 임시 URL
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

package com.yeoro.twogether.domain.diary.dto.response;

import lombok.Builder;

@Builder
public record StickerResponse(
    Long id,
    String imageUrl,
    boolean main
) {

}

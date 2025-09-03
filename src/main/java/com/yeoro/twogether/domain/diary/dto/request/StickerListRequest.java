package com.yeoro.twogether.domain.diary.dto.request;

import java.util.List;

public record StickerListRequest(
    List<StickerRequest> stickerRequests
) {

}

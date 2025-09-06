package com.yeoro.twogether.global.init;


import static com.yeoro.twogether.global.constant.AppConstants.STICKERS_FOLDER;

import com.yeoro.twogether.domain.diary.entity.StickerTemplate;
import com.yeoro.twogether.domain.diary.repository.StickerTemplateRepository;
import com.yeoro.twogether.global.service.s3.StickerS3Service;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StickerInitializer {

    private final StickerS3Service stickerS3Service;
    private final StickerTemplateRepository stickerTemplateRepository;

    @PostConstruct
    public void init() {
        List<String> urls = stickerS3Service.listStickerUrls(STICKERS_FOLDER);

        List<StickerTemplate> templates = urls.stream()
            .map(url -> StickerTemplate.builder().imageUrl(url).build())
            .toList();

        stickerTemplateRepository.saveAll(templates);
    }
}

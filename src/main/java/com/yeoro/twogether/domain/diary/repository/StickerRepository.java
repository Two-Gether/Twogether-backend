package com.yeoro.twogether.domain.diary.repository;

import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.diary.entity.Sticker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    List<Sticker> findStickersByDiary(Diary diary);

    List<Sticker> findByDiaryInAndMainTrue(List<Diary> diaries);
}

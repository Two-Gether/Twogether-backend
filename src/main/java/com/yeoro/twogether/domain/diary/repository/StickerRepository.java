package com.yeoro.twogether.domain.diary.repository;

import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.diary.entity.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    List<Sticker> findStickersByDiary(Diary diary);

    List<Sticker> findByDiaryInAndMainTrue(List<Diary> diaries);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Sticker s where s.diary.id in (:diaryIds)")
    int deleteByDiaryIds(@Param("diaryIds") List<Long> diaryIds);
}

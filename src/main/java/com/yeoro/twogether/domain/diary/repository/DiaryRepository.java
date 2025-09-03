package com.yeoro.twogether.domain.diary.repository;

import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Query("SELECT d FROM Diary d " +
        "WHERE d.member = :member " +
        "AND d.startDate <= :end " +
        "AND d.endDate >= :start")
    List<Diary> findByMemberAndStartOrEndDateInMonth(
        @Param("member") Member member,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end);
}

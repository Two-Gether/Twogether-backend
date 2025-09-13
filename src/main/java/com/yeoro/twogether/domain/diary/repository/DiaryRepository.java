package com.yeoro.twogether.domain.diary.repository;

import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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

    @Query("select d.id from Diary d where d.member.id = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Diary d where d.member.id = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}

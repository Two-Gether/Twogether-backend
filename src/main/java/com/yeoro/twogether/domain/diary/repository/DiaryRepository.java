package com.yeoro.twogether.domain.diary.repository;

import com.yeoro.twogether.domain.diary.entity.Diary;
import com.yeoro.twogether.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Query("SELECT d FROM Diary d " +
        "WHERE d.member IN :members " +
        "AND (d.startDate BETWEEN :startDate AND :endDate " +
        "OR d.endDate BETWEEN :startDate AND :endDate)")
    List<Diary> findByMembersAndDateRange(@Param("members") List<Member> members,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("select d.id from Diary d where d.member.id = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Diary d where d.member.id = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}

package com.yeoro.twogether.domain.diary.entity;

import static com.yeoro.twogether.global.exception.ErrorCode.DIARY_OWNERSHIP_MISMATCH;

import com.yeoro.twogether.domain.diary.dto.request.DiaryUpdateRequest;
import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.global.exception.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String title;

    @Column
    private Long waypointId;

    @Column
    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public Diary(LocalDate startDate, LocalDate endDate, String title,
        Long waypointId, String memo, Member member) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.title = title;
        this.waypointId = waypointId;
        this.memo = memo;
        this.member = member;
    }

    public void validateOwnership(Member member, Member partner) {
        if (!isOwnedBy(member) && !isOwnedBy(partner)) {
            throw new ServiceException(DIARY_OWNERSHIP_MISMATCH);
        }
    }

    public boolean isOwnedBy(Member member) {
        return this.member.equals(member);
    }

    public void updateDiary(DiaryUpdateRequest request) {
        this.title = request.title();
        this.startDate = request.startDate();
        this.endDate = request.endDate();
        this.waypointId = request.waypointId();
        this.memo = request.memo();
    }
}

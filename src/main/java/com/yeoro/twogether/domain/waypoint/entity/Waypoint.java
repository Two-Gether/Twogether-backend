package com.yeoro.twogether.domain.waypoint.entity;


import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_OWNERSHIP_MISMATCH;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.global.entity.BaseTime;
import com.yeoro.twogether.global.exception.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Waypoint extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waypoint_id")
    private Long id;

    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public Waypoint(String name, Member member) {
        this.name = name;
        this.member = member;
    }

    public void updateWaypoint(String name) {
        this.name = name;
    }

    public void validateMemberOwnsWaypoint(Member member) {
        if (!isOwnedBy(member) && !isOwnedBy(member.getPartner())) {
            throw new ServiceException(WAYPOINT_OWNERSHIP_MISMATCH);
        }
    }

    public boolean isOwnedBy(Member member) {
        return this.member.equals(member);
    }
}

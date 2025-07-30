package com.yeoro.twogether.domain.waypoint.entity;

import static com.yeoro.twogether.global.exception.ErrorCode.WAYPOINT_ITEM_NOT_MATCHED;

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
public class WaypointItem extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waypointItem_id")
    private Long id;

    @Column
    private String name;

    @Column
    private String address;

    @Column
    private String imageUrl;

    @Column
    private Long itemOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waypoint_id")
    private Waypoint waypoint;

    @Builder
    public WaypointItem(String name, String address, String imageUrl, Waypoint waypoint,
        Long itemOrder) {
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.waypoint = waypoint;
        this.itemOrder = itemOrder;
    }

    public void validateBelongsTo(Long waypointId) {
        if (!this.waypoint.getId().equals(waypointId)) {
            throw new ServiceException(WAYPOINT_ITEM_NOT_MATCHED);
        }
    }

    public void validateOwnedBy(Member member) {
        this.waypoint.validateMemberOwnsWaypoint(member);
    }
}

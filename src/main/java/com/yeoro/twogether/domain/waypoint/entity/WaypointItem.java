package com.yeoro.twogether.domain.waypoint.entity;

import co.elastic.clients.elasticsearch.xpack.usage.Base;
import com.yeoro.twogether.global.entity.BaseTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waypoint_id")
    private Waypoint waypoint;

    @Builder
    public WaypointItem(String name, String address, String imageUrl, Waypoint waypoint) {
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.waypoint = waypoint;
    }
}

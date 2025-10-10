package com.yeoro.twogether.domain.place.entity;

import com.yeoro.twogether.domain.member.entity.Member;
import com.yeoro.twogether.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column
    private String imageUrl;

    @Column
    private String name;

    @Column
    private String address;

    @Column
    private String description;

    @ElementCollection
    @CollectionTable(name = "place_tags", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    public static PlaceBuilder builder() { return new PlaceBuilder(); }

    public static class PlaceBuilder {
        private Member member;
        private String imageUrl;
        private String name;
        private String address;
        private String description;
        private List<String> tags = new ArrayList<>();

        public PlaceBuilder member(Member member) { this.member = member; return this; }
        public PlaceBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public PlaceBuilder name(String name) { this.name = name; return this; }
        public PlaceBuilder address(String address) { this.address = address; return this; }
        public PlaceBuilder description(String description) { this.description = description; return this; }
        public PlaceBuilder tags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); return this; }

        public Place build() {
            Place place = new Place();
            place.member = this.member;
            place.imageUrl = this.imageUrl;
            place.name = this.name;
            place.address = this.address;
            place.description = this.description;
            place.tags = this.tags;
            return place;
        }
    }

    public void overwrite(String imageUrl, String name, String description, java.util.List<String> tags) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.description = description;
        this.tags = (tags == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(tags);
    }
}

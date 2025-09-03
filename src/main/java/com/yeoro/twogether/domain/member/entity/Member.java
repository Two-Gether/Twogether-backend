package com.yeoro.twogether.domain.member.entity;

import com.yeoro.twogether.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // 실명(사용자 설정 이름)
    @Column(nullable = false)
    private String name;

    // 연인이 지어준 별명(애칭)
    @Column(nullable = true)
    private String nickname;

    @Column
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginPlatform loginPlatform;

    // OAuth 공급자별 식별자 (카카오 등). unique 허용.
    @Column(unique = true)
    private String platformId;

    @Column(nullable = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column
    private Gender gender;

    @Column
    private String ageRange; // 예: "20"

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", unique = true)
    private Member partner;

    @Column(name = "relationship_start_date", nullable = true)
    private LocalDate relationshipStartDate;

    public Long getPartnerId() {
        return partner != null ? partner.getId() : null;
    }

    /**
     * 파트너 연결 메서드
     */
    public void connectPartner(Member partner) {
        this.partner = partner;
    }

    /**
     * 비밀번호 변경 메서드
     */
    public void setPassword(String password) { this.password = password;}

    /**
     * 프로필이미지 변경 메서드
     */
    public void setProfileImageUrl(String profileImageUrl) {this.profileImageUrl = profileImageUrl;}

    /**
     * 이름 변경 메서드
     */
    public void setName(String name) {this.name = name;}

    /** 파트너 별명 변경 메서드 */
    public void setNickname(String nickname) {this.nickname = nickname;}

    public void clearRelationshipStartDate() {
        this.relationshipStartDate = null;
    }

    /**
     * 연애 시작 날짜 변경 메서드
     */
    public void changeRelationshipStartDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("날짜를 다시 입력해주세요.");
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("미래의 날짜는 선택할 수 없습니다.");
        if (Objects.equals(this.relationshipStartDate, date)) return; // idempotent
        this.relationshipStartDate = date;
    }

    @Builder
    public Member(String platformId,
                  String email,
                  String password,
                  String name,
                  String profileImageUrl,
                  LoginPlatform loginPlatform,
                  String phoneNumber,
                  Gender gender,
                  String ageRange,
                  LocalDate relationshipStartDate) {
        this.platformId = platformId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.loginPlatform = loginPlatform;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.ageRange = ageRange;
        this.relationshipStartDate = relationshipStartDate;
    }

}

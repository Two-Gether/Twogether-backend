package com.yeoro.twogether.domain.member.entity;

import com.yeoro.twogether.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginPlatform loginPlatform;

    @Column(unique = true)
    private String platformId; // 플랫폼별 고유 ID (ex: 카카오 ID)

    @Column(nullable = true, unique = true)
    private String phoneNumber;

    @Column
    private String birthday; // 예: "2000-01-01"

    @Enumerated(EnumType.STRING)
    @Column
    private Gender gender;

    @Column
    private String ageRange; // 예: "20"

    @OneToOne
    @JoinColumn(name = "partner_id")
    private Member partner;

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
     * 닉네임 변경 메서드
     */
    public void setNickname(String nickname) {this.nickname = nickname;}



    @Builder
    public Member(String platformId,
                  String email,
                  String password,
                  String nickname,
                  String profileImageUrl,
                  LoginPlatform loginPlatform,
                  String phoneNumber,
                  String birthday,
                  Gender gender,
                  String ageRange) {
        this.platformId = platformId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.loginPlatform = loginPlatform;
        this.phoneNumber = phoneNumber;
        this.birthday = birthday;
        this.gender = gender;
        this.ageRange = ageRange;
    }

}

package com.yeoro.twogether.domain.member.entity;

import com.yeoro.twogether.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginPlatform loginPlatform;

    @Column(unique = true)
    private String platformId; // 플랫폼별 고유 ID (ex: 카카오 ID)

    @OneToOne
    @JoinColumn(name = "partner_id")
    private Member partner;

    public Long getPartnerId() {
        return partner != null ? partner.getId() : null;
    }

    @Builder
    public Member(
            String platformId,
            String email,
            String nickname,
            String profileImageUrl,
            LoginPlatform loginPlatform
    ) {
        this.platformId = platformId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.loginPlatform = loginPlatform;
    }
}

package com.yeoro.twogether.domain.member.entity;

import lombok.Getter;

@Getter
public enum LoginPlatform {
        LOCAL("일반 로그인"),
        GOOGLE("구글"),
        KAKAO("카카오"),
        NAVER("네이버");

        private final String displayName;

        LoginPlatform(String displayName) {
            this.displayName = displayName;
        }

        public static LoginPlatform from(String provider) {
            if (provider == null) return LoginPlatform.LOCAL;

            switch (provider.toLowerCase()) {
                case "kakao": return KAKAO;
                case "google": return GOOGLE;
                case "naver": return NAVER;
                default: return LOCAL;
            }
        }
    }
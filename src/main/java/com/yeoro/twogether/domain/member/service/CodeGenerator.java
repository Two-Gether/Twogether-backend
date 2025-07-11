package com.yeoro.twogether.domain.member.service;

import java.security.SecureRandom;

public class CodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 파트너 연결용 6자리 코드 생성
     */
    public static String generatePartnerCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * 이메일 인증용 6자리 숫자 코드 생성
     */
    public static String generateEmailCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10)); // 0-9
        }
        return sb.toString();
    }
}


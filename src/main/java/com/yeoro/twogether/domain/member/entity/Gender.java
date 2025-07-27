package com.yeoro.twogether.domain.member.entity;

public enum Gender {
    MALE, FEMALE, UNKNOWN;

    public static Gender from(String value) {
        if (value == null) return UNKNOWN;

        return switch (value.toLowerCase()) {
            case "male" -> MALE;
            case "female" -> FEMALE;
            default -> UNKNOWN;
        };
    }
}
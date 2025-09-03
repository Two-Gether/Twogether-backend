package com.yeoro.twogether.domain.member.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    // JSON -> Enum
    @JsonCreator
    public static Gender fromJson(String value) {
        return from(value);
    }

    // Enum -> JSON
    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}

package com.yeoro.twogether.domain.member.dto.request;

import com.yeoro.twogether.domain.member.entity.Gender;
import jakarta.validation.constraints.NotNull;

public record UpdateGenderRequest(
        @NotNull Gender gender   // MALE/FEMALE/UNKNOWN
) {}
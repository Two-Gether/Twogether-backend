package com.yeoro.twogether.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAgeRangeRequest(
        @NotNull String ageRange
) {}

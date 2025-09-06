package com.yeoro.twogether.domain.member.dto.request;

public record UpdateNameRequest(
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 1, max = 30)
        String name
) {}

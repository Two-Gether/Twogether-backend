package com.yeoro.twogether.domain.member.dto.request;

public record UpdatePasswordRequest(
        @jakarta.validation.constraints.NotBlank String currentPassword,
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 8, max = 64)
        String newPassword
) {}
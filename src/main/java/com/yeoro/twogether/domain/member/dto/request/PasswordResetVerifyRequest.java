package com.yeoro.twogether.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String code
) {}

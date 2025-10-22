package com.yeoro.twogether.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetCodeRequest(
        @NotBlank @Email String email
) {}
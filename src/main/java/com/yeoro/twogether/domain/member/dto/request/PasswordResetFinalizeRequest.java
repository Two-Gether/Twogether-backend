package com.yeoro.twogether.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetFinalizeRequest(
        @NotBlank @Email String email,
        @NotBlank String resetTicket,
        @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
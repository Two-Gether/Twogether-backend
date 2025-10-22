package com.yeoro.twogether.domain.member.dto.response;

public record PasswordResetVerifyResponse(
        String resetTicket
) {}
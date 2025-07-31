package com.yeoro.twogether.domain.member.dto.request;

public record LoginRequest(
        String email,
        String password
) {}
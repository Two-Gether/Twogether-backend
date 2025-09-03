package com.yeoro.twogether.domain.member.dto.request;

import com.yeoro.twogether.domain.member.entity.Gender;

public record SignupRequest(
        String email,
        String password,
        String name,
        String phoneNumber,
        Gender gender,
        String ageRange
) {}

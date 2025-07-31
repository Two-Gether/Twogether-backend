package com.yeoro.twogether.domain.member.dto.request;
import com.yeoro.twogether.domain.member.entity.Gender;

public record SignupRequest(
        String email,
        String password,
        String nickname,
        String phoneNumber,
        String birthday,
        Gender gender,
        String ageRange
) {}
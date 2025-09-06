package com.yeoro.twogether.domain.member.dto.request;

public record UpdateProfileImageRequest(
        @jakarta.validation.constraints.NotBlank
        @org.hibernate.validator.constraints.URL
        String imageUrl
) {}

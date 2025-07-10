package com.yeoro.twogether.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // User Errors
    MEMBER_NOT_FOUND("404-1", "member.not.found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("409-1", "member.email.exists", HttpStatus.CONFLICT),
    PLATFORM_ID_ALREADY_EXISTS("409-2", "member.platformId.exists", HttpStatus.CONFLICT),
    NOT_LOCAL_MEMBER("403-1", "member.not.local", HttpStatus.FORBIDDEN),
    PASSWORD_NOT_MATCH("400-3", "member.password.not.match", HttpStatus.BAD_REQUEST),


    // Partner Errors
    PARTNER_CODE_INVALID("400-1", "partner.code.invalid", HttpStatus.BAD_REQUEST),
    SELF_PARTNER_NOT_ALLOWED("400-2", "partner.self.not.allowed", HttpStatus.BAD_REQUEST),

    // Token Errors
    TOKEN_EXPIRED("401-1", "token.expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("401-2", "token.invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_SEND_ERROR("500-3", "token.send.error", HttpStatus.INTERNAL_SERVER_ERROR),

    // Kakao OAuth Errors
    KAKAO_API_ERROR("500-1", "kakao.api.error", HttpStatus.INTERNAL_SERVER_ERROR),
    KAKAO_PROFILE_PARSE_FAILED("500-2", "kakao.profile.parse.failed", HttpStatus.INTERNAL_SERVER_ERROR),
    KAKAO_INVALID_TOKEN("401-3", "kakao.invalid.token", HttpStatus.UNAUTHORIZED);






    private final String code;
    private final String messageCode; // 메시지 프로퍼티
    private final HttpStatus status;

}

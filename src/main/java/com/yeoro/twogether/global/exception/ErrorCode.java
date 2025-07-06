package com.yeoro.twogether.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // User Errors
    MEMBER_NOT_FOUND("404-1", "member.not.found", HttpStatus.NOT_FOUND),

    // Token Errors
    TOKEN_EXPIRED("401-1", "token.expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("401-2", "token.invalid", HttpStatus.UNAUTHORIZED),

    // Waypoint Errors
    WAYPOINT_NOT_FOUND("405-1", "waypoint.not.found", HttpStatus.NOT_FOUND),
    WAYPOINT_OWNERSHIP_MISMATCH("405-2", "waypoint.ownership.mismatch", HttpStatus.FORBIDDEN);

    private final String code;
    private final String messageCode; // 메시지 프로퍼티
    private final HttpStatus status;

}

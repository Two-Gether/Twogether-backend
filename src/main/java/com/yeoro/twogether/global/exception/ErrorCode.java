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
    NOT_LOCAL_MEMBER("403-1", "member.not.local", HttpStatus.FORBIDDEN),
    PASSWORD_NOT_MATCH("400-3", "member.password.not.match", HttpStatus.BAD_REQUEST),
    CODE_GENERATION_FAILED("500-4", "partner.code.generation.failed",
        HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_NOT_VERIFIED("403-2", "member.email.not.verified", HttpStatus.FORBIDDEN),
    PASSWORD_NOT_VALID("400-4", "member.password.not.valid", HttpStatus.BAD_REQUEST),
    RELATIONSHIP_DATE_FORMAT_INVALID("400-20", "relationship.date.format.invalid",
        HttpStatus.BAD_REQUEST),
    RELATIONSHIP_DATE_RULE_VIOLATION("400-21", "relationship.date.rule.violation",
        HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD("400-5", "member.password.same.as.old", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_SET("400-6", "member.password.not.set", HttpStatus.BAD_REQUEST),

    // Partner Errors
    PARTNER_CODE_INVALID("400-1", "partner.code.invalid", HttpStatus.BAD_REQUEST),
    SELF_PARTNER_NOT_ALLOWED("400-2", "partner.self.not.allowed", HttpStatus.BAD_REQUEST),

    // Token Errors
    TOKEN_EXPIRED("401-1", "token.expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("401-2", "token.invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_SEND_ERROR("500-3", "token.send.error", HttpStatus.INTERNAL_SERVER_ERROR),

    // Kakao OAuth Errors
    KAKAO_API_ERROR("500-1", "kakao.api.error", HttpStatus.INTERNAL_SERVER_ERROR),
    KAKAO_PROFILE_PARSE_FAILED("500-2", "kakao.profile.parse.failed",
        HttpStatus.INTERNAL_SERVER_ERROR),
    KAKAO_INVALID_TOKEN("401-3", "kakao.invalid.token", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_BLACKLISTED("401-4", "token.blacklisted", HttpStatus.UNAUTHORIZED),

    // Waypoint Errors
    WAYPOINT_NOT_FOUND("405-1", "waypoint.not.found", HttpStatus.NOT_FOUND),
    WAYPOINT_OWNERSHIP_MISMATCH("405-2", "waypoint.ownership.mismatch", HttpStatus.FORBIDDEN),

    // WaypointItem Errors
    WAYPOINT_ITEM_NOT_MATCHED("406-1", "waypoint.item.not.matched", HttpStatus.BAD_REQUEST),
    WAYPOINT_ITEM_ORDER_INVALID("406-2", "waypoint.item.order.invalid", HttpStatus.BAD_REQUEST),

    // Place Errors
    PLACE_NOT_FOUND("404-10", "place.not.found", HttpStatus.NOT_FOUND),
    PLACE_ADDRESS_EXISTS("409-10", "place.address.exists", HttpStatus.CONFLICT),
    PLACE_CREATION_FAILED("500-10", "place.creation.failed", HttpStatus.INTERNAL_SERVER_ERROR),
    PLACE_TAG_LIMIT_EXCEEDED("400-11", "place.tag.limit.exceeded", HttpStatus.BAD_REQUEST),

    //Diary Errors
    DIARY_NOT_FOUND("407-01", "diary.not.found", HttpStatus.NOT_FOUND),
    DIARY_OWNERSHIP_MISMATCH("407-02", "diary.ownership.mismatch", HttpStatus.FORBIDDEN);


    private final String code;
    private final String messageCode; // 메시지 프로퍼티
    private final HttpStatus status;
}

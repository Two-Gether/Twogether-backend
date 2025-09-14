package com.yeoro.twogether.global.exception;

import com.yeoro.twogether.global.message.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;

    // ServiceException 기반
    public static ErrorResponse of(ServiceException e, HttpServletRequest request, MessageService ms) {
        String clientMessage = ms.getMessage(e.getMessageCode());
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(e.getErrorCode().getStatus().value())
                .code(e.getErrorCode().getCode())
                .message(clientMessage)
                .path(request.getRequestURI())
                .build();
    }

    // 직접 ErrorCode를 지정하는 경우
    public static ErrorResponse of(ErrorCode ec, HttpServletRequest request, MessageService ms) {
        String clientMessage = ms.getMessage(ec.getMessageCode());
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ec.getStatus().value())
                .code(ec.getCode())
                .message(clientMessage)
                .path(request.getRequestURI())
                .build();
    }
}

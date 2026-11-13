package com.petcarehub.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
        java.util.Map<String,String> fieldErrors
) {
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, currentCorrelationId(), null);
    }
    public static String currentCorrelationId() {
        var attributes=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if(attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            Object id=servlet.getRequest().getAttribute("correlationId");
            if(id instanceof String value) return value;
        }
        return java.util.UUID.randomUUID().toString();
    }
}

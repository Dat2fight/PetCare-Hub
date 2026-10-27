package com.petcarehub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Use the same error contract for filter and controller failures. */
@Component
public class SecurityErrorHandler {
    private final HandlerExceptionResolver resolver;
    public SecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }
    public void unauthorized(HttpServletRequest request, HttpServletResponse response) {
        resolver.resolveException(request, response, null,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Vui lòng đăng nhập"));
    }
    public void forbidden(HttpServletRequest request, HttpServletResponse response) {
        resolver.resolveException(request, response, null,
                new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện hành động này"));
    }
}

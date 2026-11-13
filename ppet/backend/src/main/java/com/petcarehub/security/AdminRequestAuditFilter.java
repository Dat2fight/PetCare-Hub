package com.petcarehub.security;

import com.petcarehub.service.AdminAuditService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

/** Constructed only in the security chain (never registered twice as a servlet filter). */
public class AdminRequestAuditFilter extends OncePerRequestFilter {
    private final AdminAuditService audit;
    public AdminRequestAuditFilter(AdminAuditService audit) { this.audit=audit; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(request.getAttribute("correlationId")==null) request.setAttribute("correlationId",java.util.UUID.randomUUID().toString());
        response.setHeader("X-Correlation-ID",(String)request.getAttribute("correlationId"));
        try { chain.doFilter(request,response); }
        finally {
            var auth=SecurityContextHolder.getContext().getAuthentication();
            if(request.getRequestURI().startsWith(request.getContextPath()+"/api/v1/admin/") && response.getStatus()>=400
                && auth!=null && auth.getPrincipal() instanceof UserDetailsImpl p && p.isEnabled()) {
                audit.failure(response.getStatus(),request.getMethod());
            }
        }
    }
}

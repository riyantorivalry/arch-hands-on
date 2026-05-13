package com.example.platform.common.web;

import com.example.platform.identityaccess.application.SessionAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final SessionAuthenticationService sessionAuthenticationService;

    public TenantContextFilter(SessionAuthenticationService sessionAuthenticationService) {
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = readHeader(request, CORRELATION_HEADER, UUID.randomUUID().toString());
        RequestContext context;
        try {
            context = resolveContext(request, correlationId);
        } catch (AuthenticationRequiredException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + exception.getMessage() + "\"}");
            return;
        }

        response.setHeader(CORRELATION_HEADER, correlationId);
        RequestContextHolder.set(context);
        MDC.put("correlationId", context.correlationId());
        MDC.put("tenantId", valueOrDash(context.tenantId()));
        MDC.put("workspaceId", valueOrDash(context.workspaceId()));
        MDC.put("userId", valueOrDash(context.userId()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
            RequestContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    private String readHeader(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? fallback : value;
    }

    private RequestContext resolveContext(HttpServletRequest request, String correlationId) {
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            return new RequestContext(null, null, null, correlationId);
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationRequiredException("Bearer token is required");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return sessionAuthenticationService.authenticate(token, correlationId);
    }

    private boolean isPublicEndpoint(String path) {
        return "/api/tenants".equals(path) || "/api/auth/login".equals(path);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

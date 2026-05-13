package com.example.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String WORKSPACE_HEADER = "X-Workspace-Id";
    public static final String USER_HEADER = "X-User-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = readHeader(request, CORRELATION_HEADER, UUID.randomUUID().toString());
        RequestContext context = new RequestContext(
                readHeader(request, TENANT_HEADER, "tenant-dev"),
                readHeader(request, WORKSPACE_HEADER, "workspace-dev"),
                readHeader(request, USER_HEADER, "user-dev"),
                correlationId
        );

        response.setHeader(CORRELATION_HEADER, correlationId);
        RequestContextHolder.set(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }

    private String readHeader(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? fallback : value;
    }
}

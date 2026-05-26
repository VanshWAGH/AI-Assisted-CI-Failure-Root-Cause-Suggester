package com.rootcause.web.filter;

import com.rootcause.service.ApiKeyAuditService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * API-key authentication filter.
 *
 * <p>Checks the {@code X-API-Key} header on all protected routes.
 * Public routes (actuator, swagger, webhooks) are exempt.
 *
 * <p>Enable by setting {@code rootcause.api.keys} in application.yml
 * to a comma-separated list of valid keys. If the list is empty
 * the filter is disabled (dev-mode permissive).
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyAuditService apiKeyAuditService;

    /** Comma-separated list of valid API keys (empty = disabled). */
    @Value("${rootcause.api.keys:}")
    private String rawKeys;

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/webhooks", "/api/v1/status"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Skip auth for public paths
        String uri = httpReq.getRequestURI();
        if (isPublicPath(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = httpReq.getHeader(API_KEY_HEADER);

        // If no keys configured, allow everything (dev mode)
        if (rawKeys == null || rawKeys.isBlank()) {
            try {
                chain.doFilter(request, response);
            } finally {
                if (providedKey != null && !providedKey.isBlank()) {
                    String ipAddress = getClientIp(httpReq);
                    String userAgent = httpReq.getHeader("User-Agent");
                    apiKeyAuditService.auditKeyUsage(providedKey, uri, ipAddress, userAgent, httpResp.getStatus());
                }
            }
            return;
        }

        Set<String> validKeys = Set.of(rawKeys.split(","));

        if (providedKey == null || !validKeys.contains(providedKey.trim())) {
            log.warn("Rejected request to {} — invalid or missing API key", uri);
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-API-Key header\"}"
            );
            if (providedKey != null && !providedKey.isBlank()) {
                String ipAddress = getClientIp(httpReq);
                String userAgent = httpReq.getHeader("User-Agent");
                apiKeyAuditService.auditKeyUsage(providedKey, uri, ipAddress, userAgent, HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (providedKey != null && !providedKey.isBlank()) {
                String ipAddress = getClientIp(httpReq);
                String userAgent = httpReq.getHeader("User-Agent");
                apiKeyAuditService.auditKeyUsage(providedKey, uri, ipAddress, userAgent, httpResp.getStatus());
            }
        }
    }

    private boolean isPublicPath(String uri) {
        return PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


package com.rootcause.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Logs a concise one-liner for every inbound HTTP request and its outcome.
 *
 * <p>Example log line:
 * <pre>
 * POST /api/v1/analyze/raw 200 OK 47ms [requestId=abc-123]
 * </pre>
 */
@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        long startMs = Instant.now().toEpochMilli();

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = Instant.now().toEpochMilli() - startMs;
            log.info("{} {} {} {}ms",
                    httpReq.getMethod(),
                    httpReq.getRequestURI(),
                    httpResp.getStatus(),
                    durationMs);
        }
    }
}

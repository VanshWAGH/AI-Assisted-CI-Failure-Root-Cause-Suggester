package com.rootcause.service;

import com.rootcause.domain.ApiKeyAudit;
import com.rootcause.repository.ApiKeyAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuditService {

    private final ApiKeyAuditRepository auditRepository;

    /**
     * Records an API key access audit entry asynchronously.
     */
    @Async("taskExecutor")
    @Transactional
    public void auditKeyUsage(String rawKey, String endpoint, String ipAddress, String userAgent, int statusCode) {
        try {
            if (rawKey == null || rawKey.isBlank()) {
                return;
            }

            // Extract first 12 characters safely
            String cleanKey = rawKey.trim();
            String prefix = cleanKey.length() <= 12 ? cleanKey : cleanKey.substring(0, 12);

            ApiKeyAudit audit = ApiKeyAudit.builder()
                    .keyPrefix(prefix)
                    .endpoint(truncate(endpoint, 200))
                    .ipAddress(truncate(ipAddress, 45))
                    .userAgent(truncate(userAgent, 500))
                    .statusCode(statusCode)
                    .build();

            auditRepository.save(audit);
            log.debug("Successfully audited key usage prefix={} for endpoint={}", prefix, endpoint);
        } catch (Exception e) {
            log.error("Failed to write API key audit log: {}", e.getMessage(), e);
        }
    }

    private String truncate(String val, int max) {
        if (val == null) return null;
        return val.length() <= max ? val : val.substring(0, max);
    }
}

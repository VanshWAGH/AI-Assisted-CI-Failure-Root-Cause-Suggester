package com.rootcause;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rootcause.domain.ApiKeyAudit;
import com.rootcause.repository.ApiKeyAuditRepository;
import com.rootcause.repository.CiJobRepository;
import com.rootcause.repository.FailurePatternRepository;
import com.rootcause.repository.JobFailureAnalysisRepository;
import com.rootcause.classifier.RuleBasedFailureClassifier;
import com.rootcause.web.dto.AnalyzeRawRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "rootcause.api.keys=valid-test-key-123456")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API Key Auditing and Authentication Integration Tests")
class ApiKeyAuditTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyAuditRepository auditRepository;

    @Autowired
    private FailurePatternRepository patternRepository;

    @Autowired
    private JobFailureAnalysisRepository analysisRepository;

    @Autowired
    private CiJobRepository ciJobRepository;

    @Autowired
    private RuleBasedFailureClassifier classifier;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        ciJobRepository.deleteAll();
        auditRepository.deleteAll();
        patternRepository.deleteAll();

        // Seed patterns so rule-based analysis can run successfully
        com.rootcause.domain.FailurePattern infraPattern = Objects.requireNonNull(com.rootcause.domain.FailurePattern.builder()
                .name("Connection Refused")
                .failureType(com.rootcause.domain.enums.FailureType.INFRA)
                .regexPattern("(?i)(connection\\s+refused)")
                .explanationTemplate("Connection refused: {matched_text}")
                .suggestedActionTemplate("Check connectivity and retry.")
                .priority(90)
                .build());
        patternRepository.save(infraPattern);

        classifier.reloadPatterns();
    }

    @Test
    @DisplayName("Should successfully authenticate request and record audit log with 200 status")
    void shouldAuditSuccessfulRequest() throws Exception {
        AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                .projectName("audit-success-test")
                .ciPlatform("GITLAB")
                .logContent("Connection refused")
                .build();

        mockMvc.perform(post("/api/v1/analyze/raw")
                .header("X-API-Key", "valid-test-key-123456")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Wait since database audit write is asynchronous
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ApiKeyAudit> audits = auditRepository.findAll();
            assertThat(audits, hasSize(1));
            ApiKeyAudit audit = audits.get(0);
            assertThat(audit.getKeyPrefix(), is("valid-test-k")); // first 12 chars of valid-test-key-123456
            assertThat(audit.getEndpoint(), is("/api/v1/analyze/raw"));
            assertThat(audit.getStatusCode(), is(200));
            assertThat(audit.getIpAddress(), notNullValue());
        });
    }

    @Test
    @DisplayName("Should reject unauthorized request and record audit log with 401 status when key is provided")
    void shouldAuditUnauthorizedRequestWithKey() throws Exception {
        AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                .projectName("audit-auth-fail-test")
                .ciPlatform("GITLAB")
                .logContent("Some logs")
                .build();

        mockMvc.perform(post("/api/v1/analyze/raw")
                .header("X-API-Key", "wrong-key-value-999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ApiKeyAudit> audits = auditRepository.findAll();
            assertThat(audits, hasSize(1));
            ApiKeyAudit audit = audits.get(0);
            assertThat(audit.getKeyPrefix(), is("wrong-key-va")); // first 12 chars of wrong-key-value-999
            assertThat(audit.getEndpoint(), is("/api/v1/analyze/raw"));
            assertThat(audit.getStatusCode(), is(401));
        });
    }

    @Test
    @DisplayName("Should reject unauthorized request but NOT record audit log when no key header is provided")
    void shouldNotAuditUnauthorizedRequestWithoutKey() throws Exception {
        AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                .projectName("audit-no-key-test")
                .ciPlatform("GITLAB")
                .logContent("Some logs")
                .build();

        mockMvc.perform(post("/api/v1/analyze/raw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Since no key is provided, we should never write an audit log
        Thread.sleep(1000); // Wait to ensure async execution has had time to run if it mistakenly did
        List<ApiKeyAudit> audits = auditRepository.findAll();
        assertThat(audits, empty());
    }
}

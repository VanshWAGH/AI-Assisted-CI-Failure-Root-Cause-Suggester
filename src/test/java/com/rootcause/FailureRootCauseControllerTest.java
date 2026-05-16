package com.rootcause;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rootcause.domain.CiJob;
import com.rootcause.domain.CiJobLog;
import com.rootcause.domain.FailurePattern;
import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.domain.enums.JobStatus;
import com.rootcause.classifier.RuleBasedFailureClassifier;
import com.rootcause.repository.CiJobLogRepository;
import com.rootcause.repository.CiJobRepository;
import com.rootcause.repository.FailurePatternRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Failure Root-Cause Controller Integration Tests")
class FailureRootCauseControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private CiJobRepository ciJobRepository;
        @Autowired
        private CiJobLogRepository ciJobLogRepository;
        @Autowired
        private FailurePatternRepository patternRepository;
        @Autowired
        private RuleBasedFailureClassifier classifier;

        @BeforeEach
        void setUp() {
                ciJobLogRepository.deleteAll();
                ciJobRepository.deleteAll();
                patternRepository.deleteAll();

                // Seed patterns
                FailurePattern infraPattern = Objects.requireNonNull(FailurePattern.builder()
                                .name("Connection Refused")
                                .failureType(FailureType.INFRA)
                                .regexPattern("(?i)(connection\\s+refused)")
                                .explanationTemplate("Connection refused: {matched_text}")
                                .suggestedActionTemplate("Check connectivity and retry.")
                                .priority(90)
                                .build());
                patternRepository.save(infraPattern);

                FailurePattern testPattern = Objects.requireNonNull(FailurePattern.builder()
                                .name("Test Failure")
                                .failureType(FailureType.TEST)
                                .regexPattern("(?i)(Tests?\\s+run:.*Failures?:\\s*[1-9])")
                                .explanationTemplate("Tests failed: {matched_text}")
                                .suggestedActionTemplate("Review test output.")
                                .priority(70)
                                .build());
                patternRepository.save(testPattern);

                classifier.reloadPatterns();
        }

        @Test
        @DisplayName("POST /api/v1/analyze/raw should return analysis for infra failure")
        void shouldAnalyzeRawInfraFailure() throws Exception {
                AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                                .projectName("my-org/my-repo")
                                .pipelineRef("12345")
                                .branchName("main")
                                .commitSha("abc123def456")
                                .ciPlatform("GITLAB")
                                .logContent("Starting app...\njava.net.ConnectException: Connection refused\nBuild failed.")
                                .build();

                mockMvc.perform(post("/api/v1/analyze/raw")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.failureType", Objects.requireNonNull(is("infra"))))
                                .andExpect(jsonPath("$.confidence", greaterThan(0.0)))
                                .andExpect(jsonPath("$.explanation",
                                                Objects.requireNonNull(containsString("Connection refused"))))
                                .andExpect(jsonPath("$.suggestedAction", notNullValue()))
                                .andExpect(jsonPath("$.classifierMode", Objects.requireNonNull(is("rule_based"))))
                                .andExpect(jsonPath("$.analysisId", notNullValue()))
                                .andExpect(jsonPath("$.jobId", notNullValue()));
        }

        @Test
        @DisplayName("POST /api/v1/analyze/raw should work with logLines array")
        void shouldAnalyzeRawWithLogLines() throws Exception {
                AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                                .projectName("test-project")
                                .ciPlatform("JENKINS")
                                .logLines(List.of(
                                                "[INFO] Running tests...",
                                                "Tests run: 10, Failures: 2, Errors: 0, Skipped: 0",
                                                "[ERROR] Build finished with failures"))
                                .build();

                mockMvc.perform(post("/api/v1/analyze/raw")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.failureType", Objects.requireNonNull(is("test"))))
                                .andExpect(jsonPath("$.confidence", greaterThan(0.0)));
        }

        @Test
        @DisplayName("POST /api/v1/jobs/{jobId}/analyze should analyze stored logs")
        void shouldAnalyzeStoredJob() throws Exception {
                // Create a job with logs
                CiJob job = Objects.requireNonNull(ciJobRepository.save(Objects.requireNonNull(CiJob.builder()
                                .externalJobId("job-999")
                                .ciPlatform(CiPlatform.GITLAB)
                                .projectName("test-org/test-repo")
                                .status(JobStatus.FAILED)
                                .build())));

                ciJobLogRepository.save(Objects.requireNonNull(CiJobLog.builder()
                                .job(job)
                                .logSource("console")
                                .logContent("java.net.ConnectException: Connection refused (Connection refused)")
                                .build()));

                mockMvc.perform(post("/api/v1/jobs/{jobId}/analyze", job.getId())
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content("{}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.failureType", Objects.requireNonNull(is("infra"))))
                                .andExpect(jsonPath("$.jobId", Objects.requireNonNull(is(job.getId().toString()))));
        }

        @Test
        @DisplayName("POST /api/v1/jobs/{jobId}/analyze should return 400 for non-existent job")
        void shouldReturn400ForMissingJob() throws Exception {
                mockMvc.perform(post("/api/v1/jobs/00000000-0000-0000-0000-000000000001/analyze")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", Objects.requireNonNull(containsString("not found"))));
        }

        @Test
        @DisplayName("GET /api/v1/status should return service info")
        void shouldReturnStatus() throws Exception {
                mockMvc.perform(get("/api/v1/status"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.service", Objects.requireNonNull(is("rootcause-suggester"))))
                                .andExpect(jsonPath("$.status", Objects.requireNonNull(is("UP"))));
        }

        @Test
        @DisplayName("GET /api/v1/jobs/{jobId}/report should return Markdown report")
        void shouldReturnMarkdownReport() throws Exception {
                // Create and analyze a job first
                AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                                .projectName("report-test")
                                .ciPlatform("GITLAB")
                                .logContent("Connection refused at database:5432")
                                .build();

                String analysisJson = mockMvc.perform(post("/api/v1/analyze/raw")
                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                String jobId = objectMapper.readTree(analysisJson).get("jobId").asText();

                // Get report
                mockMvc.perform(get("/api/v1/jobs/{jobId}/report", jobId))
                                .andExpect(status().isOk())
                                .andExpect(content().string(Objects.requireNonNull(
                                                containsString("CI Failure Root-Cause Analysis Report"))));
        }
}
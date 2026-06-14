package com.rootcause;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rootcause.domain.FailurePattern;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.integration.github.GitHubApiClient;
import com.rootcause.repository.CiJobLogRepository;
import com.rootcause.repository.CiJobRepository;
import com.rootcause.repository.FailurePatternRepository;
import com.rootcause.classifier.RuleBasedFailureClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GitHub Webhook Controller Integration Tests")
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CiJobRepository ciJobRepository;

    @Autowired
    private CiJobLogRepository ciJobLogRepository;

    @Autowired
    private FailurePatternRepository patternRepository;

    @Autowired
    private RuleBasedFailureClassifier classifier;

    @Autowired
    private GitHubApiClient gitHubApiClient;

    @Value("${rootcause.github.webhook-secret}")
    private String webhookSecret;

    private TestGitHubApiClient mockClient;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        @Primary
        public GitHubApiClient gitHubApiClient() {
            return new TestGitHubApiClient();
        }
    }

    public static class TestGitHubApiClient extends GitHubApiClient {
        public List<WorkflowJob> failedJobsResponse = new ArrayList<>();
        public String jobLogResponse = "";

        public String lastCommentProjectId;
        public String lastCommentPrNumber;
        public String lastCommentBody;

        public boolean getFailedJobsCalled = false;
        public boolean fetchJobLogCalled = false;
        public boolean postCommentCalled = false;

        public TestGitHubApiClient() {
            super(WebClient.builder(), "http://localhost:9997", "token");
        }

        public void reset() {
            failedJobsResponse = new ArrayList<>();
            jobLogResponse = "";
            lastCommentProjectId = null;
            lastCommentPrNumber = null;
            lastCommentBody = null;
            getFailedJobsCalled = false;
            fetchJobLogCalled = false;
            postCommentCalled = false;
        }

        @Override
        public List<WorkflowJob> getFailedJobs(String owner, String repo, Long runId) {
            getFailedJobsCalled = true;
            return failedJobsResponse;
        }

        @Override
        public String fetchJobLog(String owner, String repo, String jobId) {
            fetchJobLogCalled = true;
            return jobLogResponse;
        }

        @Override
        public void postAnalysisComment(String projectId, String pullRequestNumber, String commentBody) {
            postCommentCalled = true;
            lastCommentProjectId = projectId;
            lastCommentPrNumber = pullRequestNumber;
            lastCommentBody = commentBody;
        }
    }

    @BeforeEach
    void setUp() {
        mockClient = (TestGitHubApiClient) gitHubApiClient;
        mockClient.reset();

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
        classifier.reloadPatterns();
    }

    private String calculateSignature(String payload, String secret) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return "sha256=" + hexString.toString();
    }

    @Test
    @DisplayName("POST /webhooks/github/workflow-run should reject request with missing signature")
    void shouldRejectMissingSignature() throws Exception {
        String payload = "{}";
        mockMvc.perform(post("/webhooks/github/workflow-run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Invalid signature"));
    }

    @Test
    @DisplayName("POST /webhooks/github/workflow-run should reject request with invalid signature")
    void shouldRejectInvalidSignature() throws Exception {
        String payload = "{}";
        mockMvc.perform(post("/webhooks/github/workflow-run")
                        .header("X-Hub-Signature-256", "sha256=invalid-signature-hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Invalid signature"));
    }

    @Test
    @DisplayName("POST /webhooks/github/workflow-run should ignore successful workflow runs")
    void shouldIgnoreSuccessfulWorkflow() throws Exception {
        String payload = """
                {
                    "action": "completed",
                    "workflow_run": {
                        "id": 12345,
                        "status": "completed",
                        "conclusion": "success"
                    },
                    "repository": {
                        "name": "my-app",
                        "full_name": "my-org/my-app",
                        "owner": {
                            "login": "my-org"
                        }
                    }
                }
                """;

        String signature = calculateSignature(payload, webhookSecret);

        mockMvc.perform(post("/webhooks/github/workflow-run")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ignored"));

        assertFalse(mockClient.getFailedJobsCalled);
    }

    @Test
    @DisplayName("POST /webhooks/github/workflow-run should accept failed workflow run and execute analysis workflow")
    void shouldAcceptFailedWorkflowAndAnalyze() throws Exception {
        String payload = """
                {
                    "action": "completed",
                    "workflow_run": {
                        "id": 12345,
                        "status": "completed",
                        "conclusion": "failure",
                        "head_branch": "main",
                        "head_sha": "abcdef123456",
                        "pull_requests": [
                            {
                                "id": 9876,
                                "number": 12
                            }
                        ]
                    },
                    "repository": {
                        "name": "my-app",
                        "full_name": "my-org/my-app",
                        "owner": {
                            "login": "my-org"
                        }
                    }
                }
                """;

        String signature = calculateSignature(payload, webhookSecret);

        // Configure test response in mock client
        GitHubApiClient.WorkflowJob job = new GitHubApiClient.WorkflowJob();
        job.setId(54321L);
        job.setName("build");
        job.setStatus("completed");
        job.setConclusion("failure");

        mockClient.failedJobsResponse.add(job);
        mockClient.jobLogResponse = "Starting build...\njava.net.ConnectException: Connection refused (Connection refused)\nBuild failed.";

        // Perform request
        mockMvc.perform(post("/webhooks/github/workflow-run")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Wait a brief moment or call the verification directly since async execution in tests 
        // with mockMvc might execute on a separate thread if @Async is enabled and MockMvc isn't blocking.
        // Wait, did it complete? Let's check with standard thread sleeping or polling to prevent race conditions.
        int attempts = 0;
        while (attempts < 50 && !mockClient.postCommentCalled) {
            Thread.sleep(100);
            attempts++;
        }

        // Verify log fetching and PR comment posting
        assertTrue(mockClient.getFailedJobsCalled, "getFailedJobs should have been called");
        assertTrue(mockClient.fetchJobLogCalled, "fetchJobLog should have been called");
        assertTrue(mockClient.postCommentCalled, "postAnalysisComment should have been called");

        assertEquals("my-org/my-app", mockClient.lastCommentProjectId);
        assertEquals("12", mockClient.lastCommentPrNumber);
        assertNotNull(mockClient.lastCommentBody);
        assertTrue(mockClient.lastCommentBody.contains("INFRA"));
        assertTrue(mockClient.lastCommentBody.contains("Connection refused"));
    }
}

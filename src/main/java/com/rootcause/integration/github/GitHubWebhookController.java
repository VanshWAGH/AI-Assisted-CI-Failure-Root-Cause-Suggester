package com.rootcause.integration.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rootcause.service.FailureClassificationService;
import com.rootcause.web.dto.AnalysisResponse;
import com.rootcause.web.dto.AnalyzeRawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Webhook receiver for GitHub Actions workflow_run events.
 */
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitHub Webhook", description = "GitHub Actions workflow_run event receiver")
public class GitHubWebhookController {

    private final FailureClassificationService classificationService;
    private final GitHubApiClient gitHubApiClient;
    private final ObjectMapper objectMapper;

    @Value("${rootcause.github.webhook-secret:change-me}")
    private String webhookSecret;

    /**
     * Receive and process GitHub workflow_run webhook event.
     */
    @PostMapping("/workflow-run")
    @Operation(
            summary = "Receive GitHub workflow_run webhook",
            description = "Accepts workflow completion events and triggers async log analysis for failures"
    )
    public ResponseEntity<Map<String, Object>> handleWorkflowRun(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody String rawPayload) {

        // Validate HMAC-SHA256 signature
        if (!isValidSignature(rawPayload, signatureHeader)) {
            log.warn("Rejected GitHub webhook — invalid HMAC signature");
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Invalid signature", "status", 403));
        }

        GitHubWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, GitHubWebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse GitHub webhook JSON: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Malformed payload", "status", 400));
        }

        // Only handle completed failed runs
        if (!payload.isFailedWorkflowRun()) {
            String conclusion = payload.getWorkflowRun() != null ? payload.getWorkflowRun().getConclusion() : "none";
            String action = payload.getAction();
            log.debug("Ignoring GitHub webhook event (action={}, conclusion={})", action, conclusion);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "not a failed workflow run completion"));
        }

        String owner = payload.getRepository().getOwner().getLogin();
        String repo = payload.getRepository().getName();
        Long runId = payload.getWorkflowRun().getId();

        log.info("Queuing async analysis for failed GitHub workflow run {} in {}/{}", runId, owner, repo);
        processFailedWorkflowAsync(payload, owner, repo, runId);

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "message", "analysis queued"));
    }

    /**
     * Async workflow processing — fetches logs and posts PR feedback in the background.
     */
    @Async("webhookExecutor")
    public void processFailedWorkflowAsync(GitHubWebhookPayload payload, String owner, String repo, Long runId) {
        try {
            // Find all failed jobs for this workflow run
            List<GitHubApiClient.WorkflowJob> failedJobs = gitHubApiClient.getFailedJobs(owner, repo, runId);
            if (failedJobs.isEmpty()) {
                log.info("No failed jobs found in GitHub workflow run {} — skipping log fetch", runId);
                return;
            }

            for (GitHubApiClient.WorkflowJob job : failedJobs) {
                String jobIdStr = String.valueOf(job.getId());
                String logContent = gitHubApiClient.fetchJobLog(owner, repo, jobIdStr);

                if (logContent == null || logContent.isBlank()) {
                    log.warn("Empty log for GitHub job {} in {}/{} — skipping", jobIdStr, owner, repo);
                    continue;
                }

                // Truncate to 1 MB size limit to protect memory
                if (logContent.length() > 1_048_576) {
                    log.warn("Log for GitHub job {} is too large ({} chars). Truncating to 1 MB.", jobIdStr, logContent.length());
                    logContent = logContent.substring(logContent.length() - 1_048_576);
                }

                AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                        .projectName(payload.getRepository().getFullName())
                        .pipelineRef(String.valueOf(runId))
                        .branchName(payload.getWorkflowRun().getHeadBranch())
                        .commitSha(payload.getWorkflowRun().getHeadSha())
                        .ciPlatform("GITHUB")
                        .logContent(logContent)
                        .build();

                AnalysisResponse response = classificationService.analyzeRaw(request);

                // If PRs are linked, post suggestion feedback comment
                if (payload.getWorkflowRun().getPullRequests() != null &&
                        !payload.getWorkflowRun().getPullRequests().isEmpty()) {

                    GitHubWebhookPayload.PullRequestInfo pr = payload.getWorkflowRun().getPullRequests().get(0);
                    if (pr.getNumber() != null) {
                        String comment = gitHubApiClient.formatPrComment(
                                response.getFailureType(),
                                response.getConfidence(),
                                response.getExplanation(),
                                response.getSuggestedAction()
                        );

                        gitHubApiClient.postAnalysisComment(
                                payload.getRepository().getFullName(),
                                String.valueOf(pr.getNumber()),
                                comment
                        );
                    }
                }

                log.info("Async analysis done for GitHub job {}: type={}, confidence={:.2f}",
                        jobIdStr, response.getFailureType(), response.getConfidence());
            }

        } catch (Exception e) {
            log.error("Failed to async process GitHub workflow run {}: {}", runId, e.getMessage(), e);
        }
    }

    /**
     * Compute HMAC-SHA256 and compare it with the X-Hub-Signature-256 header.
     */
    private boolean isValidSignature(String payload, String signatureHeader) {
        if ("change-me".equals(webhookSecret)) {
            return true; // Bypass signature validation in local/dev mode
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }

        String expectedSignature = signatureHeader.substring(7);
        try {
            SecretKeySpec signingKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(expectedSignature);
        } catch (Exception e) {
            log.error("Error validating HMAC-SHA256 signature for GitHub webhook: {}", e.getMessage());
            return false;
        }
    }
}

package com.rootcause.integration.gitlab;

import com.rootcause.service.FailureClassificationService;
import com.rootcause.web.dto.AnalysisResponse;
import com.rootcause.web.dto.AnalyzeRawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import java.util.Arrays;
import java.util.List;

/**
 * Webhook receiver for GitLab CI pipeline events.
 *
 * Setup in GitLab:
 * Settings → Webhooks → Add webhook
 * URL: https://your-api.example.com/webhooks/gitlab/pipeline
 * Trigger: Pipeline events
 * Secret token: (configured in application.yml)
 */
@RestController
@RequestMapping("/webhooks/gitlab")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitLab Webhook", description = "GitLab CI pipeline event receiver")
public class GitLabWebhookController {

    private final FailureClassificationService classificationService;
    private final GitLabApiClient gitLabApiClient;

    @Value("${rootcause.gitlab.webhook-secret:change-me}")
    private String webhookSecret;

    /**
     * Handle GitLab pipeline webhook events.
     * GitLab sends POST with X-Gitlab-Token header for authentication.
     */
    @PostMapping("/pipeline")
    @Operation(summary = "Receive GitLab pipeline webhook", description = "Processes pipeline failure events and posts MR comments")
    public ResponseEntity<?> handlePipelineEvent(
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestBody GitLabWebhookPayload payload) {

        // Verify webhook secret
        if (!webhookSecret.equals("change-me") && !webhookSecret.equals(token)) {
            log.warn("Invalid webhook token received");
            return ResponseEntity.status(403).body("Invalid token");
        }

        // Only process pipeline events
        if (!"pipeline".equals(payload.getObjectKind())) {
            log.debug("Ignoring non-pipeline event: {}", payload.getObjectKind());
            return ResponseEntity.ok("Ignored: not a pipeline event");
        }

        // Only process failures
        if (!payload.isPipelineFailure()) {
            log.debug("Ignoring non-failure pipeline: {}", payload.getObjectAttributes().getStatus());
            return ResponseEntity.ok("Ignored: pipeline did not fail");
        }

        log.info("Processing pipeline failure: project={}, pipeline={}, ref={}",
                payload.getProject().getPathWithNamespace(),
                payload.getObjectAttributes().getId(),
                payload.getObjectAttributes().getRef());

        // Process each failed job
        List<Long> failedJobIds = payload.getFailedBuildIds();
        if (failedJobIds.isEmpty()) {
            log.info("No failed jobs found in pipeline, skipping analysis");
            return ResponseEntity.ok("No failed jobs to analyze");
        }

        // Analyze each failed job asynchronously
        for (Long jobId : failedJobIds) {
            processFailedJob(payload, jobId);
        }

        return ResponseEntity.ok("Processing " + failedJobIds.size() + " failed job(s)");
    }

    private void processFailedJob(GitLabWebhookPayload payload, Long jobId) {
        try {
            Long projectId = payload.getProject().getId();

            // Fetch job log from GitLab API
            String logContent = gitLabApiClient.fetchJobLog(projectId, jobId);
            if (logContent == null || logContent.isBlank()) {
                log.warn("Empty log for job {} in project {}", jobId, projectId);
                return;
            }

            // Run analysis
            AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                    .projectName(payload.getProject().getPathWithNamespace())
                    .pipelineRef(String.valueOf(payload.getObjectAttributes().getId()))
                    .branchName(payload.getObjectAttributes().getRef())
                    .commitSha(payload.getCommit() != null ? payload.getCommit().getId() : null)
                    .ciPlatform("GITLAB")
                    .logContent(logContent)
                    .build();

            AnalysisResponse response = classificationService.analyzeRaw(request);

            // Post MR comment if this pipeline is associated with a merge request
            if (payload.getMergeRequest() != null && payload.getMergeRequest().getIid() != null) {
                String comment = gitLabApiClient.formatMrComment(
                        response.getFailureType(),
                        response.getConfidence(),
                        response.getExplanation(),
                        response.getSuggestedAction());

                gitLabApiClient.postAnalysisComment(
                        String.valueOf(projectId),
                        String.valueOf(payload.getMergeRequest().getIid()),
                        comment);
            }

            log.info("Analysis complete for GitLab job {}: type={}, confidence={}",
                    jobId, response.getFailureType(), String.format("%.2f", response.getConfidence()));

        } catch (Exception e) {
            log.error("Failed to process GitLab job {}: {}", jobId, e.getMessage(), e);
        }
    }
}

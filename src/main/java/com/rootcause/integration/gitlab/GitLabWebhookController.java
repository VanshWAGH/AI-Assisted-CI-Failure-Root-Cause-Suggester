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
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Webhook receiver for GitLab CI pipeline events.
 *
 * <p>Webhook processing is fully asynchronous — the controller returns
 * {@code 202 Accepted} immediately while the jobs are analysed in the
 * background on the {@code webhookExecutor} thread pool, keeping GitLab's
 * 10 s delivery timeout well clear.
 *
 * <p>Setup in GitLab:
 * <pre>
 * Settings → Webhooks → Add webhook
 * URL: https://your-api.example.com/webhooks/gitlab/pipeline
 * Trigger: Pipeline events
 * Secret token: (configured via GITLAB_WEBHOOK_SECRET env var)
 * </pre>
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
     *
     * <p>Returns {@code 202 Accepted} immediately; analysis runs async.
     */
    @PostMapping("/pipeline")
    @Operation(
            summary = "Receive GitLab pipeline webhook",
            description = "Accepts pipeline failure events and triggers async root-cause analysis"
    )
    public ResponseEntity<Map<String, Object>> handlePipelineEvent(
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestBody GitLabWebhookPayload payload) {

        // Verify webhook secret
        if (!isDefaultSecret() && !webhookSecret.equals(token)) {
            log.warn("Rejected webhook — invalid X-Gitlab-Token from project {}",
                    payload.getProject() != null ? payload.getProject().getPathWithNamespace() : "unknown");
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Invalid token", "status", 403));
        }

        // Only handle pipeline events
        if (!"pipeline".equals(payload.getObjectKind())) {
            log.debug("Ignoring non-pipeline event: {}", payload.getObjectKind());
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "not a pipeline event"));
        }

        // Only handle failures
        if (!payload.isPipelineFailure()) {
            String pipelineStatus = payload.getObjectAttributes() != null
                    ? payload.getObjectAttributes().getStatus() : "unknown";
            log.debug("Ignoring non-failure pipeline status: {}", pipelineStatus);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "pipeline did not fail"));
        }

        List<Long> failedJobIds = payload.getFailedBuildIds();
        if (failedJobIds.isEmpty()) {
            log.info("Pipeline failure received but no individual jobs failed — skipping");
            return ResponseEntity.accepted()
                    .body(Map.of("status", "accepted", "jobsQueued", 0));
        }

        log.info("Queuing async analysis for {} failed job(s) in pipeline {} (project={})",
                failedJobIds.size(),
                payload.getObjectAttributes().getId(),
                payload.getProject().getPathWithNamespace());

        // Fire-and-forget — runs on webhookExecutor pool
        for (Long jobId : failedJobIds) {
            processFailedJobAsync(payload, jobId);
        }

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "jobsQueued", failedJobIds.size()));
    }

    /**
     * Async job analysis — runs off the HTTP thread so GitLab never times out.
     */
    @Async("webhookExecutor")
    public void processFailedJobAsync(GitLabWebhookPayload payload, Long jobId) {
        try {
            Long projectId = payload.getProject().getId();

            String logContent = gitLabApiClient.fetchJobLog(projectId, jobId);
            if (logContent == null || logContent.isBlank()) {
                log.warn("Empty log for job {} in project {} — skipping", jobId, projectId);
                return;
            }

            // Enforce a reasonable log size cap (1 MB)
            if (logContent.length() > 1_048_576) {
                log.warn("Log for job {} is {} chars — truncating to 1 MB", jobId, logContent.length());
                logContent = logContent.substring(logContent.length() - 1_048_576);
            }

            AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                    .projectName(payload.getProject().getPathWithNamespace())
                    .pipelineRef(String.valueOf(payload.getObjectAttributes().getId()))
                    .branchName(payload.getObjectAttributes().getRef())
                    .commitSha(payload.getCommit() != null ? payload.getCommit().getId() : null)
                    .ciPlatform("GITLAB")
                    .logContent(logContent)
                    .build();

            AnalysisResponse response = classificationService.analyzeRaw(request);

            // Post MR comment when the pipeline is linked to a merge request
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

            log.info("Async analysis done for GitLab job {}: type={}, confidence={:.2f}",
                    jobId, response.getFailureType(), response.getConfidence());

        } catch (Exception e) {
            log.error("Async processing failed for GitLab job {}: {}", jobId, e.getMessage(), e);
        }
    }

    private boolean isDefaultSecret() {
        return "change-me".equals(webhookSecret);
    }
}

package com.rootcause.integration.jenkins;

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

import java.util.Map;

/**
 * Webhook receiver for Jenkins build notifications.
 *
 * <p>Requires the <b>Jenkins Notification Plugin</b> to be installed in Jenkins.
 * Configure the plugin to POST to:
 * <pre>
 *   http://&lt;your-api-host&gt;:8080/webhooks/jenkins/build
 * </pre>
 *
 * <p>Optionally set a shared secret token via the {@code JENKINS_WEBHOOK_SECRET}
 * environment variable. Jenkins will send it in the {@code X-Jenkins-Token} header.
 *
 * <p>Processing is fully asynchronous — returns {@code 202 Accepted} immediately
 * while the analysis runs in the background.
 */
@RestController
@RequestMapping("/webhooks/jenkins")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jenkins Webhook", description = "Jenkins build event receiver")
public class JenkinsWebhookController {

    private final FailureClassificationService classificationService;
    private final JenkinsLogFetcher jenkinsLogFetcher;

    @Value("${rootcause.jenkins.webhook-secret:}")
    private String webhookSecret;

    // ── Endpoint ──────────────────────────────────────────────────────────────

    /**
     * Receive Jenkins build event and trigger async root-cause analysis on failure.
     *
     * <p>Returns {@code 202 Accepted} immediately. Analysis runs on the
     * {@code webhookExecutor} thread pool.
     */
    @PostMapping("/build")
    @Operation(
            summary     = "Receive Jenkins build webhook",
            description = "Accepts Jenkins Notification Plugin events and triggers async root-cause analysis on build failure"
    )
    public ResponseEntity<Map<String, Object>> handleBuildEvent(
            @RequestHeader(value = "X-Jenkins-Token", required = false) String token,
            @RequestBody JenkinsBuildPayload payload) {

        // Optional secret verification
        if (!webhookSecret.isBlank() && !webhookSecret.equals(token)) {
            log.warn("Rejected Jenkins webhook — invalid X-Jenkins-Token for job '{}'", payload.getJobName());
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Invalid token", "status", 403));
        }

        String jobName     = payload.getJobName();
        String buildNumber = payload.getBuildNumberStr();
        String phase       = payload.getBuild() != null ? payload.getBuild().getPhase() : "unknown";
        String status      = payload.getBuild() != null ? payload.getBuild().getStatus() : "unknown";

        log.debug("Jenkins webhook received: job={}, build={}, phase={}, status={}",
                jobName, buildNumber, phase, status);

        // Only process failure events
        if (!payload.isBuildFailure()) {
            log.debug("Ignoring non-failure Jenkins event: phase={}, status={}", phase, status);
            return ResponseEntity.ok(Map.of(
                    "status", "ignored",
                    "reason", "not a build failure (phase=" + phase + ", status=" + status + ")"
            ));
        }

        log.info("Jenkins build failure detected: job={}, build={} — queuing async analysis", jobName, buildNumber);

        // Fire-and-forget async analysis
        analyzeJenkinsBuildAsync(payload);

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "job", jobName, "build", buildNumber));
    }

    // ── Async processing ──────────────────────────────────────────────────────

    /**
     * Asynchronously fetch the Jenkins console log and run root-cause analysis.
     */
    @Async("webhookExecutor")
    public void analyzeJenkinsBuildAsync(JenkinsBuildPayload payload) {
        String jobName     = payload.getJobName();
        String buildNumber = payload.getBuildNumberStr();

        try {
            // Prefer inline log if the webhook payload already contains it (Generic Webhook Trigger)
            String logContent = null;
            if (payload.getBuild() != null && payload.getBuild().getLog() != null
                    && !payload.getBuild().getLog().isBlank()) {
                logContent = payload.getBuild().getLog();
                log.info("Using inline log from webhook payload for job={}, build={}", jobName, buildNumber);
            } else {
                // Fetch from Jenkins REST API  →  /job/{jobName}/{buildNumber}/consoleText
                logContent = jenkinsLogFetcher.fetchJobLog(jobName + "/" + buildNumber);
            }

            if (logContent == null || logContent.isBlank()) {
                log.warn("Empty log for Jenkins job={}, build={} — skipping analysis", jobName, buildNumber);
                return;
            }

            // Enforce 1 MB cap (keep tail — most relevant for failures)
            if (logContent.length() > 1_048_576) {
                log.warn("Jenkins log for job={} is {} chars — truncating to 1 MB", jobName, logContent.length());
                logContent = logContent.substring(logContent.length() - 1_048_576);
            }

            AnalyzeRawRequest request = AnalyzeRawRequest.builder()
                    .projectName(jobName)
                    .pipelineRef(buildNumber)
                    .branchName(payload.getBranchName())
                    .commitSha(payload.getCommitSha())
                    .ciPlatform("JENKINS")
                    .logContent(logContent)
                    .build();

            AnalysisResponse response = classificationService.analyzeRaw(request);

            log.info("Jenkins analysis done: job={}, build={}, type={}, confidence={:.2f}",
                    jobName, buildNumber, response.getFailureType(), response.getConfidence());

            // Update Jenkins build description with analysis result
            jenkinsLogFetcher.postAnalysisComment(
                    jobName,
                    buildNumber,
                    jenkinsLogFetcher.formatBuildDescription(
                            response.getFailureType(),
                            response.getConfidence(),
                            response.getExplanation(),
                            response.getSuggestedAction()
                    )
            );

        } catch (Exception e) {
            log.error("Async Jenkins analysis failed for job={}, build={}: {}", jobName, buildNumber, e.getMessage(), e);
        }
    }
}

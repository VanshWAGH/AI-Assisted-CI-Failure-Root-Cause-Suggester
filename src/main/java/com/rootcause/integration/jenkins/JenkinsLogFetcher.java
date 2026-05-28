package com.rootcause.integration.jenkins;

import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.integration.CiPlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Base64;
import java.util.Map;

/**
 * Jenkins API client for fetching build logs and updating build descriptions.
 *
 * <p>Uses the Jenkins REST API:
 * <ul>
 *   <li>GET  /job/{jobName}/{buildNumber}/consoleText  — fetch console output</li>
 *   <li>POST /job/{jobName}/{buildNumber}/submitDescription — update build description</li>
 * </ul>
 *
 * <p>Configure via environment variables:
 * <pre>
 *   JENKINS_BASE_URL   = http://your-jenkins:8080
 *   JENKINS_USERNAME   = your-jenkins-user
 *   JENKINS_API_TOKEN  = your-api-token (preferred) or password
 * </pre>
 */
@Component
@Slf4j
public class JenkinsLogFetcher implements CiPlatformAdapter {

    private final WebClient webClient;
    private final boolean credentialsConfigured;

    public JenkinsLogFetcher(
            WebClient.Builder webClientBuilder,
            @Value("${rootcause.jenkins.base-url:http://localhost:8081}") String baseUrl,
            @Value("${rootcause.jenkins.username:}") String username,
            @Value("${rootcause.jenkins.api-token:}") String apiToken) {

        WebClient.Builder builder = webClientBuilder
                .baseUrl(baseUrl != null ? baseUrl : "http://localhost:8081");

        boolean hasCredentials = username != null && !username.isBlank()
                && apiToken != null && !apiToken.isBlank();

        if (hasCredentials) {
            String auth = Base64.getEncoder()
                    .encodeToString((username + ":" + apiToken).getBytes());
            builder.defaultHeader("Authorization", "Basic " + auth);
            log.info("Jenkins client configured: baseUrl={}, username={}", baseUrl, username);
        } else {
            log.warn("Jenkins client running WITHOUT credentials — set JENKINS_USERNAME and JENKINS_API_TOKEN in .env");
        }

        this.webClient = builder.build();
        this.credentialsConfigured = hasCredentials;
    }

    // ── CiPlatformAdapter ─────────────────────────────────────────────────────

    /**
     * Fetch console output for a Jenkins build.
     * GET /job/{jobName}/{buildNumber}/consoleText
     *
     * @param externalJobId format: "jobName/buildNumber"  (e.g. "my-pipeline/42")
     */
    @Override
    public String fetchJobLog(String externalJobId) {
        String[] parts = externalJobId.split("/", 2);
        if (parts.length != 2) {
            log.warn("Invalid Jenkins externalJobId '{}'. Expected 'jobName/buildNumber'", externalJobId);
            return "";
        }
        return fetchConsoleText(parts[0], parts[1]);
    }

    /**
     * Update the Jenkins build description with the analysis result.
     *
     * <p>Jenkins exposes POST /job/{jobName}/{buildNumber}/submitDescription
     * which sets the human-readable description shown in the build history.
     *
     * @param projectId      Jenkins job name / path (e.g. "my-pipeline")
     * @param buildNumber    Jenkins build number (e.g. "42")
     * @param description    HTML or plain-text description to set
     */
    @Override
    public void postAnalysisComment(String projectId, String buildNumber, String description) {
        if (!credentialsConfigured) {
            log.warn("Cannot update Jenkins build description — no credentials configured");
            return;
        }

        try {
            webClient.post()
                    .uri("/job/{job}/{build}/submitDescription", projectId, buildNumber)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("description=" + java.net.URLEncoder.encode(description, java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Updated Jenkins build description for job={}, build={}", projectId, buildNumber);

        } catch (WebClientResponseException e) {
            log.error("Failed to update Jenkins build description for job={}, build={}: HTTP {} {}",
                    projectId, buildNumber, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update Jenkins build description for job={}, build={}: {}",
                    projectId, buildNumber, e.getMessage());
        }
    }

    @Override
    public CiPlatform platform() {
        return CiPlatform.JENKINS;
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    /**
     * Fetch console text by separate job name + build number.
     */
    public String fetchConsoleText(String jobName, String buildNumber) {
        try {
            String logContent = webClient.get()
                    .uri("/job/{jobName}/{buildNumber}/consoleText", jobName, buildNumber)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            int length = logContent != null ? logContent.length() : 0;
            log.info("Fetched {} chars of log for Jenkins job={}, build={}", length, jobName, buildNumber);
            return logContent != null ? logContent : "";

        } catch (WebClientResponseException e) {
            log.error("HTTP {} fetching Jenkins log for job={}, build={}: {}",
                    e.getStatusCode(), jobName, buildNumber, e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("Failed to fetch Jenkins log for job={}, build={}: {}", jobName, buildNumber, e.getMessage());
            return "";
        }
    }

    /**
     * Format the analysis result as a concise plain-text Jenkins build description.
     * Jenkins build descriptions support basic HTML.
     */
    public String formatBuildDescription(String failureType, double confidence,
                                         String explanation, String suggestedAction) {
        String pct = String.format("%.0f%%", confidence * 100);
        String type = failureType != null ? failureType.toUpperCase() : "UNKNOWN";
        String shortExplanation = explanation != null && explanation.length() > 200
                ? explanation.substring(0, 197) + "..."
                : (explanation != null ? explanation : "N/A");
        String shortAction = suggestedAction != null && suggestedAction.length() > 200
                ? suggestedAction.substring(0, 197) + "..."
                : (suggestedAction != null ? suggestedAction : "N/A");

        return String.format(
                "🔍 <b>%s</b> (%s confidence)<br/>" +
                "<i>%s</i><br/>" +
                "💡 %s",
                type, pct, shortExplanation, shortAction
        );
    }
}
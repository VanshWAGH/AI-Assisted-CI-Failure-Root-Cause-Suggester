package com.rootcause.integration.jenkins;

import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.integration.CiPlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
// import java.util.Map;

/**
 * Jenkins API client for fetching build logs.
 * Uses Jenkins REST API to retrieve console output.
 */
@Component
@Slf4j
public class JenkinsLogFetcher implements CiPlatformAdapter {

    private final WebClient webClient;

    public JenkinsLogFetcher(
            WebClient.Builder webClientBuilder,
            @Value("${rootcause.jenkins.base-url:http://localhost:8081}") String baseUrl,
            @Value("${rootcause.jenkins.username:}") String username,
            @Value("${rootcause.jenkins.api-token:}") String apiToken) {

        WebClient.Builder builder = webClientBuilder.baseUrl(baseUrl != null ? baseUrl : "http://localhost:8081");

        // Add Basic Auth if credentials are provided
        if (username != null && !username.isBlank() && apiToken != null && !apiToken.isBlank()) {
            String auth = Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
            builder.defaultHeader("Authorization", "Basic " + auth);
        }

        this.webClient = builder.build();
    }

    /**
     * Fetch console output for a Jenkins build.
     * GET /job/{jobName}/{buildNumber}/consoleText
     *
     * @param externalJobId format: "jobName/buildNumber"
     */
    @Override
    public String fetchJobLog(String externalJobId) {
        String[] parts = externalJobId.split("/");
        if (parts.length != 2) {
            log.warn("Invalid Jenkins externalJobId format: {}. Expected 'jobName/buildNumber'", externalJobId);
            return "";
        }

        try {
            String logContent = webClient.get()
                    .uri("/job/{jobName}/{buildNumber}/consoleText", parts[0], parts[1])
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Fetched {} chars of log for Jenkins job {}", logContent != null ? logContent.length() : 0,
                    externalJobId);
            return logContent != null ? logContent : "";
        } catch (Exception e) {
            log.error("Failed to fetch Jenkins job log for {}: {}", externalJobId, e.getMessage());
            return "";
        }
    }

    @Override
    public void postAnalysisComment(String projectId, String mergeRequestId, String commentBody) {
        // Jenkins doesn't have native MR comments — this would integrate with
        // a Git provider (GitHub/GitLab) or post to a Jenkins build description.
        log.info("Jenkins comment posting not implemented — log analysis result instead");
        log.info("Analysis for project {}: {}", projectId,
                commentBody.substring(0, Math.min(200, commentBody.length())));
    }

    @Override
    public CiPlatform platform() {
        return CiPlatform.JENKINS;
    }
}
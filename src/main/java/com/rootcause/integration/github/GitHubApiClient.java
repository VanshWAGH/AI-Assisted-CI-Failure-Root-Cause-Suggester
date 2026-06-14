package com.rootcause.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.integration.CiPlatformAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * GitHub API client for fetching job logs and posting Pull Request comments.
 */
@Component
@Slf4j
public class GitHubApiClient implements CiPlatformAdapter {

    private final WebClient webClient;

    public GitHubApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${rootcause.github.api-url:https://api.github.com}") String apiUrl,
            @Value("${rootcause.github.token:}") String githubToken) {

        this.webClient = webClientBuilder
                .baseUrl(apiUrl != null ? apiUrl : "https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    /**
     * Fetch logs for a specific GitHub Actions job.
     * externalJobId format: "owner/repo/jobId"
     */
    @Override
    public String fetchJobLog(String externalJobId) {
        String[] parts = externalJobId.split("/");
        if (parts.length != 3) {
            log.warn("Invalid externalJobId format for GitHub: {}. Expected 'owner/repo/jobId'", externalJobId);
            return "";
        }
        return fetchJobLog(parts[0], parts[1], parts[2]);
    }

    /**
     * Fetch job log by owner, repo, and jobId.
     */
    public String fetchJobLog(String owner, String repo, String jobId) {
        try {
            // GET /repos/{owner}/{repo}/actions/jobs/{job_id}/logs
            String logContent = webClient.get()
                    .uri("/repos/{owner}/{repo}/actions/jobs/{jobId}/logs", owner, repo, jobId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Fetched {} chars of log for GitHub job {}/{} ({})", 
                    logContent != null ? logContent.length() : 0, owner, repo, jobId);
            return logContent != null ? logContent : "";
        } catch (Exception e) {
            log.error("Failed to fetch GitHub job log for {}/{} (job {}): {}", owner, repo, jobId, e.getMessage());
            return "";
        }
    }

    /**
     * Get the list of failed jobs for a given workflow run.
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs
     */
    public List<WorkflowJob> getFailedJobs(String owner, String repo, Long runId) {
        try {
            WorkflowJobsResponse response = webClient.get()
                    .uri("/repos/{owner}/{repo}/actions/runs/{runId}/jobs", owner, repo, runId)
                    .retrieve()
                    .bodyToMono(WorkflowJobsResponse.class)
                    .block();

            if (response == null || response.getJobs() == null) {
                return List.of();
            }

            return response.getJobs().stream()
                    .filter(job -> "failure".equalsIgnoreCase(job.getConclusion()))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch jobs list for workflow run {} in {}/{}: {}", runId, owner, repo, e.getMessage());
            return List.of();
        }
    }

    /**
     * Post a comment on a GitHub Pull Request (Issue comments endpoint).
     * POST /repos/{owner}/{repo}/issues/{issue_number}/comments
     */
    @Override
    public void postAnalysisComment(String projectId, String pullRequestNumber, String commentBody) {
        // projectId is expected to be "owner/repo"
        String[] parts = projectId.split("/");
        if (parts.length != 2) {
            log.warn("Invalid projectId format: {}. Expected 'owner/repo'", projectId);
            return;
        }

        try {
            Map<String, String> body = Map.of("body", commentBody != null ? commentBody : "");
            webClient.post()
                    .uri("/repos/{owner}/{repo}/issues/{issueNumber}/comments", parts[0], parts[1], pullRequestNumber)
                    .bodyValue(Objects.requireNonNull(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Posted analysis comment on Pull Request #{} in repository {}", pullRequestNumber, projectId);
        } catch (Exception e) {
            log.error("Failed to post GitHub comment on Pull Request #{} in {}: {}", pullRequestNumber, projectId, e.getMessage());
        }
    }

    /**
     * Format the classification result as a GitHub-friendly markdown comment.
     */
    public String formatPrComment(String failureType, double confidence, String explanation, String suggestedAction) {
        return String.format("""
                ## 🔍 CI Failure Root-Cause Analysis
                
                | Field | Value |
                |-------|-------|
                | **Failure Type** | `%s` |
                | **Confidence** | %.0f%% |
                
                ### 📋 Explanation
                
                %s
                
                ### 💡 Suggested Action
                
                %s
                
                ---
                *Generated by [RootCause Suggester](https://github.com/your-org/rootcause-suggester) • Powered by AI*
                """,
                failureType.toUpperCase(),
                confidence * 100,
                explanation,
                suggestedAction);
    }

    @Override
    public CiPlatform platform() {
        return CiPlatform.GITHUB;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowJobsResponse {
        private List<WorkflowJob> jobs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowJob {
        private Long id;
        private String name;
        private String status;      // e.g. "completed"
        private String conclusion;  // e.g. "failure", "success"
    }
}

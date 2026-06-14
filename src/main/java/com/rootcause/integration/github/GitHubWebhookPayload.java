package com.rootcause.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Deserialization model for GitHub Actions workflow_run webhook events.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubWebhookPayload {

    private String action; // e.g. "completed"

    @JsonProperty("workflow_run")
    private WorkflowRunInfo workflowRun;

    private RepositoryInfo repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowRunInfo {
        private Long id;
        private String name;
        private String status; // e.g. "completed"
        private String conclusion; // e.g. "failure", "success"

        @JsonProperty("head_branch")
        private String headBranch;

        @JsonProperty("head_sha")
        private String headSha;

        @JsonProperty("html_url")
        private String htmlUrl;

        @JsonProperty("pull_requests")
        private List<PullRequestInfo> pullRequests;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryInfo {
        private Long id;
        private String name;

        @JsonProperty("full_name")
        private String fullName; // e.g. "owner/repo"

        private OwnerInfo owner;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OwnerInfo {
        private String login;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestInfo {
        private Long id;
        private Integer number;
    }

    /**
     * Check if this webhook corresponds to a failed workflow run completion.
     */
    public boolean isFailedWorkflowRun() {
        return "completed".equalsIgnoreCase(action) &&
                workflowRun != null &&
                "failure".equalsIgnoreCase(workflowRun.getConclusion());
    }
}

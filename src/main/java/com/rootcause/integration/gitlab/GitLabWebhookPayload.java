package com.rootcause.integration.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Deserialization model for GitLab Pipeline webhook events.
 * Ref: https://docs.gitlab.com/ee/user/project/integrations/webhook_events.html#pipeline-events
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabWebhookPayload {

    @JsonProperty("object_kind")
    private String objectKind; // "pipeline"

    @JsonProperty("object_attributes")
    private PipelineAttributes objectAttributes;

    private ProjectInfo project;

    private CommitInfo commit;

    @JsonProperty("merge_request")
    private MergeRequestInfo mergeRequest;

    private List<BuildInfo> builds;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PipelineAttributes {
        private Long id;
        private String ref;
        private String status;  // "failed", "success", etc.
        private String source;

        @JsonProperty("detailed_status")
        private String detailedStatus;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("finished_at")
        private String finishedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectInfo {
        private Long id;
        private String name;

        @JsonProperty("path_with_namespace")
        private String pathWithNamespace;

        @JsonProperty("web_url")
        private String webUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitInfo {
        private String id;  // SHA
        private String message;

        @JsonProperty("author_name")
        private String authorName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MergeRequestInfo {
        private Long id;
        private Long iid;
        private String title;
        private String state;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuildInfo {
        private Long id;
        private String name;
        private String stage;
        private String status;  // "failed", "success", etc.

        @JsonProperty("allow_failure")
        private boolean allowFailure;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("finished_at")
        private String finishedAt;
    }

    /**
     * Check if this is a pipeline failure event.
     */
    public boolean isPipelineFailure() {
        return objectAttributes != null && "failed".equalsIgnoreCase(objectAttributes.getStatus());
    }

    /**
     * Get all failed build/job IDs.
     */
    public List<Long> getFailedBuildIds() {
        if (builds == null) return List.of();
        return builds.stream()
                .filter(b -> "failed".equalsIgnoreCase(b.getStatus()) && !b.isAllowFailure())
                .map(BuildInfo::getId)
                .toList();
    }
}

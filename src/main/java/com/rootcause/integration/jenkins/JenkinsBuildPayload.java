package com.rootcause.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Deserialization model for Jenkins Notification Plugin webhook payloads.
 *
 * <p>Jenkins sends this JSON body when the "Notification Plugin" is installed
 * and configured with a URL endpoint. The payload structure follows the
 * Jenkins Notification Plugin format:
 * https://plugins.jenkins.io/notification/
 *
 * <p>Also supports the Generic Webhook Trigger plugin format.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JenkinsBuildPayload {

    /** Build name / job name */
    private String name;

    /** Full URL to the Jenkins job */
    private String url;

    /** Build details */
    private Build build;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Build {

        /** Build number */
        private Integer number;

        /** Build phase: STARTED, COMPLETED, FINALIZED */
        private String phase;

        /**
         * Build status: SUCCESS, FAILURE, UNSTABLE, ABORTED.
         * Only present when phase = COMPLETED or FINALIZED.
         */
        private String status;

        /** URL to this specific build */
        private String url;

        /** Full URL including Jenkins base URL */
        @JsonProperty("full_url")
        private String fullUrl;

        /** Git SCM info */
        private ScmInfo scm;

        /** Build parameters */
        private java.util.Map<String, Object> parameters;

        /** Log as plain text (for Generic Webhook Trigger) */
        @JsonProperty("log")
        private String log;

        /** Branch name (for Generic Webhook Trigger) */
        @JsonProperty("branch")
        private String branch;

        /** Commit SHA (for Generic Webhook Trigger) */
        @JsonProperty("commit")
        private String commit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScmInfo {
        private String url;
        private String branch;
        private String commit;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    /**
     * Returns true if this event represents a build failure that should be analyzed.
     */
    public boolean isBuildFailure() {
        if (build == null) return false;
        String status = build.getStatus();
        String phase  = build.getPhase();
        // Only analyze on COMPLETED or FINALIZED phase with FAILURE / UNSTABLE status
        boolean completedPhase = "COMPLETED".equalsIgnoreCase(phase)
                || "FINALIZED".equalsIgnoreCase(phase);
        boolean failedStatus = "FAILURE".equalsIgnoreCase(status)
                || "UNSTABLE".equalsIgnoreCase(status);
        return completedPhase && failedStatus;
    }

    /**
     * Returns the build number as a string, or "unknown".
     */
    public String getBuildNumberStr() {
        return build != null && build.getNumber() != null
                ? String.valueOf(build.getNumber())
                : "unknown";
    }

    /**
     * Returns the job name / path (e.g. "my-pipeline" or "folder/my-job").
     */
    public String getJobName() {
        return name != null ? name : "unknown";
    }

    /**
     * Returns the Git branch from SCM info or build parameter, or "unknown".
     */
    public String getBranchName() {
        if (build == null) return "unknown";
        if (build.getScm() != null && build.getScm().getBranch() != null) {
            return build.getScm().getBranch();
        }
        if (build.getBranch() != null) return build.getBranch();
        return "unknown";
    }

    /**
     * Returns the Git commit SHA from SCM info, or "unknown".
     */
    public String getCommitSha() {
        if (build == null) return "unknown";
        if (build.getScm() != null && build.getScm().getCommit() != null) {
            return build.getScm().getCommit();
        }
        if (build.getCommit() != null) return build.getCommit();
        return "unknown";
    }
}

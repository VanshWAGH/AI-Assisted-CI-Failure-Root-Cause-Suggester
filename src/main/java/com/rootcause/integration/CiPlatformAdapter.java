package com.rootcause.integration;

import com.rootcause.domain.enums.CiPlatform;

import java.util.List;

/**
 * Common interface for CI platform adapters.
 */
public interface CiPlatformAdapter {

    /**
     * Fetch log content for a given external job ID.
     */
    String fetchJobLog(String externalJobId);

    /**
     * Post an analysis comment on the merge/pull request.
     */
    void postAnalysisComment(String projectId, String mergeRequestId, String commentBody);

    /**
     * Which CI platform this adapter handles.
     */
    CiPlatform platform();
}

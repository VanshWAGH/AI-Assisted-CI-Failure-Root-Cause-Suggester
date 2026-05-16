package com.rootcause.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

/**
 * Request body for POST /api/v1/analyze/raw
 * Accepts raw log content for analysis without a pre-existing job.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeRawRequest {

    /** Project identifier, e.g. "my-org/my-repo" */
    private String projectName;

    /** Pipeline or build number */
    private String pipelineRef;

    /** Branch or MR ref name */
    private String branchName;

    /** Git commit SHA */
    private String commitSha;

    /** CI platform: GITLAB, JENKINS, OTHER */
    private String ciPlatform;

    /** Log source type: console, junit, surefire */
    private String logSource;

    /** Raw log content as a single string */
    private String logContent;

    /** Log content as individual lines (alternative to logContent) */
    private List<String> logLines;

    /** Classification mode: RULE_BASED, ML_ONNX, or HYBRID */
    private String mode;
}

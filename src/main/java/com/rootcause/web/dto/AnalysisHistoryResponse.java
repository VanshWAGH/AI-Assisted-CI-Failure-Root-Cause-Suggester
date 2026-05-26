package com.rootcause.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight analysis summary used in the paginated history endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisHistoryResponse {

    private UUID analysisId;
    private UUID jobId;
    private String externalJobId;
    private String projectName;
    private String branchName;

    /** Classification result: infra, test, build, security, unknown */
    private String failureType;

    /** Confidence score 0.0 – 1.0 */
    private Double confidence;

    /** Which classifier was used: rule_based, ml_onnx, hybrid */
    private String classifierMode;

    /** Name of the matched pattern (if rule-based) */
    private String matchedPatternName;

    private Instant analyzedAt;
}

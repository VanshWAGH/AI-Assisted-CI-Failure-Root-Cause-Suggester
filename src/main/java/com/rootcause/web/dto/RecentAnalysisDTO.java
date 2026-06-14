package com.rootcause.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentAnalysisDTO {
    private UUID analysisId;
    private UUID jobId;
    private String externalJobId;
    private String projectName;
    private String branchName;
    private String failureType;
    private Double confidence;
    private String classifierMode;
    private String matchedPatternName;
    private String explanation;
    private String suggestedAction;
    private Instant analyzedAt;
}

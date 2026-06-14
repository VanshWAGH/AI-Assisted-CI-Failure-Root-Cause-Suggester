package com.rootcause.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** Read-only DTO for exposing FailurePattern over REST. */
@Data
@Builder
public class FailurePatternDTO {
    private UUID    id;
    private String  name;
    private String  failureType;
    private String  regexPattern;
    private String  explanationTemplate;
    private String  suggestedActionTemplate;
    private Integer priority;
    private Boolean active;
    private Instant createdAt;
}

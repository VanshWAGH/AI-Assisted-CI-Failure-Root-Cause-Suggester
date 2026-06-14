package com.rootcause.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsDTO {
    private long totalFailuresToday;
    private long infraFailures;
    private long testFailures;
    private long buildFailures;
    private long securityFailures;
    private long unknownFailures;
    private double avgConfidence;
    private Instant lastUpdated;
}

package com.rootcause.web;

import com.rootcause.service.AnalysisReportService;
import com.rootcause.service.FailureClassificationService;
import com.rootcause.web.dto.AnalysisResponse;
import com.rootcause.web.dto.AnalyzeRawRequest;
import com.rootcause.web.dto.AnalyzeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for CI failure root-cause analysis.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Failure Analysis", description = "CI failure root-cause analysis endpoints")
public class FailureRootCauseController {

    private final FailureClassificationService classificationService;
    private final AnalysisReportService reportService;

    /**
     * Analyze a previously ingested CI job by its internal ID.
     */
    @PostMapping("/jobs/{jobId}/analyze")
    @Operation(summary = "Analyze a CI job failure", description = "Runs failure classification on stored logs for a given job ID")
    public ResponseEntity<AnalysisResponse> analyzeJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AnalyzeRequest request) {

        AnalysisResponse response = classificationService.analyzeJob(jobId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze raw log content directly (no pre-existing job required).
     * This is the primary endpoint for CI integrations.
     */
    @PostMapping("/analyze/raw")
    @Operation(summary = "Analyze raw CI logs", description = "Accepts raw log content and returns failure analysis")
    public ResponseEntity<AnalysisResponse> analyzeRaw(
            @Valid @RequestBody AnalyzeRawRequest request) {

        AnalysisResponse response = classificationService.analyzeRaw(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a Markdown report for a job analysis.
     */
    @GetMapping(value = "/jobs/{jobId}/report", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "Get analysis report", description = "Returns a Markdown-formatted analysis report for a job")
    public ResponseEntity<String> getJobReport(@PathVariable UUID jobId) {
        String report = reportService.generateJobReport(jobId);
        return ResponseEntity.ok(report);
    }

    /**
     * Get a summary report for a project.
     */
    @GetMapping(value = "/projects/{projectName}/summary", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "Get project failure summary", description = "Returns a Markdown-formatted summary of failures for a project")
    public ResponseEntity<String> getProjectSummary(@PathVariable String projectName) {
        String report = reportService.generateProjectSummary(projectName);
        return ResponseEntity.ok(report);
    }

    /**
     * Health check / info endpoint.
     */
    @GetMapping("/status")
    @Operation(summary = "Service status", description = "Returns service status information")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(java.util.Map.of(
                "service", "rootcause-suggester",
                "status", "UP",
                "version", "1.0.0-SNAPSHOT",
                "timestamp", java.time.Instant.now()
        ));
    }
}

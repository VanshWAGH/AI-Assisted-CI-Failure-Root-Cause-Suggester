package com.rootcause.web;

import com.rootcause.service.AnalysisReportService;
import com.rootcause.service.FailureClassificationService;
import com.rootcause.service.HistoryService;
import com.rootcause.web.dto.AnalysisResponse;
import com.rootcause.web.dto.AnalysisHistoryResponse;
import com.rootcause.web.dto.AnalyzeRawRequest;
import com.rootcause.web.dto.AnalyzeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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
    private final HistoryService historyService;

    // ── Analysis endpoints ────────────────────────────────────────────────────

    /**
     * Analyze raw log content directly (no pre-existing job required).
     * This is the primary endpoint for CI integrations.
     */
    @PostMapping("/analyze/raw")
    @Operation(
            summary = "Analyze raw CI logs",
            description = "Accepts raw log content (≤ 1 MB) and returns failure analysis with suggested fix"
    )
    public ResponseEntity<AnalysisResponse> analyzeRaw(
            @Valid @RequestBody AnalyzeRawRequest request) {

        guardLogSize(request);
        AnalysisResponse response = classificationService.analyzeRaw(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze a previously ingested CI job by its internal ID.
     */
    @PostMapping("/jobs/{jobId}/analyze")
    @Operation(
            summary = "Analyze a CI job failure",
            description = "Runs failure classification on stored logs for a given job UUID"
    )
    public ResponseEntity<AnalysisResponse> analyzeJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AnalyzeRequest request) {

        AnalysisResponse response = classificationService.analyzeJob(jobId, request);
        return ResponseEntity.ok(response);
    }

    // ── Report endpoints ──────────────────────────────────────────────────────

    /**
     * Get a Markdown report for a job analysis.
     */
    @GetMapping(value = "/jobs/{jobId}/report", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(
            summary = "Get analysis report",
            description = "Returns a Markdown-formatted analysis report for a job"
    )
    public ResponseEntity<String> getJobReport(@PathVariable UUID jobId) {
        String report = reportService.generateJobReport(jobId);
        return ResponseEntity.ok(report);
    }

    /**
     * Get a summary report for a project.
     */
    @GetMapping(value = "/projects/{projectName}/summary", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(
            summary = "Get project failure summary",
            description = "Returns a Markdown-formatted summary of recent failures for a project"
    )
    public ResponseEntity<String> getProjectSummary(@PathVariable String projectName) {
        String report = reportService.generateProjectSummary(projectName);
        return ResponseEntity.ok(report);
    }

    // ── History endpoint ──────────────────────────────────────────────────────

    /**
     * Paginated analysis history (newest first).
     */
    @GetMapping("/history")
    @Operation(
            summary = "Paginated analysis history",
            description = "Returns a paginated list of all failure analyses ordered by most recent first"
    )
    public ResponseEntity<Page<AnalysisHistoryResponse>> getHistory(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by failure type, e.g. infra, test, build, security")
            @RequestParam(required = false) String type) {

        size = Math.min(size, 100); // cap
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "analyzedAt"));
        Page<AnalysisHistoryResponse> history = historyService.getHistory(pageRequest, type);
        return ResponseEntity.ok(history);
    }

    // ── Status ────────────────────────────────────────────────────────────────

    /**
     * Health check / info endpoint.
     */
    @GetMapping("/status")
    @Operation(summary = "Service status", description = "Returns service status information")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service",   "rootcause-suggester",
                "status",    "UP",
                "version",   "1.0.0",
                "timestamp", java.time.Instant.now()
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reject payloads larger than 1 MB to prevent memory exhaustion. */
    private void guardLogSize(AnalyzeRawRequest request) {
        if (request.getLogContent() != null && request.getLogContent().length() > 1_048_576) {
            throw new IllegalArgumentException(
                    "logContent exceeds the 1 MB limit (" + request.getLogContent().length() + " chars). " +
                    "Send only the relevant portion of the log."
            );
        }
    }
}

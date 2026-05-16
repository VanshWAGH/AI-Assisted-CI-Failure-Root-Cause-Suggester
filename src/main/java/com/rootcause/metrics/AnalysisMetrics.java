package com.rootcause.metrics;

import com.rootcause.classifier.ClassificationResult;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom Prometheus / Micrometer metrics for the root-cause analysis service.
 */
@Component
@Slf4j
public class AnalysisMetrics {

    private final Counter jobFailureTotal;
    private final MeterRegistry registry;

    public AnalysisMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.jobFailureTotal = Counter.builder("rootcause_job_failure_total")
                .description("Total failure events received")
                .register(registry);
    }

    /**
     * Record metrics for a completed analysis.
     */
    public void recordAnalysis(ClassificationResult result, double durationSeconds) {
        // Increment total failure count
        jobFailureTotal.increment();

        // Count by failure type
        Counter.builder("rootcause_failure_type_count")
                .tag("failure_type", result.getFailureType().name().toLowerCase())
                .description("Count per failure category")
                .register(registry)
                .increment();

        // Record confidence as histogram
        DistributionSummary.builder("rootcause_model_confidence")
                .tag("classifier_mode", result.getClassifierMode().name().toLowerCase())
                .tag("failure_type", result.getFailureType().name().toLowerCase())
                .description("Confidence score distribution")
                .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)
                .register(registry)
                .record(result.getConfidence());

        // Record analysis duration
        Timer.builder("rootcause_analyze_duration_seconds")
                .tag("classifier_mode", result.getClassifierMode().name().toLowerCase())
                .tag("status", "success")
                .description("Time to complete analysis")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record((long) (durationSeconds * 1_000_000_000), TimeUnit.NANOSECONDS);

        // Count matched pattern (if rule-based)
        if (result.getMatchedPatternName() != null) {
            Counter.builder("rootcause_pattern_match_count")
                    .tag("pattern_name", result.getMatchedPatternName())
                    .description("How often each pattern fires")
                    .register(registry)
                    .increment();
        }
    }

    /**
     * Record a failed analysis attempt.
     */
    public void recordAnalysisError(String classifierMode, double durationSeconds) {
        Timer.builder("rootcause_analyze_duration_seconds")
                .tag("classifier_mode", classifierMode)
                .tag("status", "error")
                .description("Time to complete analysis")
                .register(registry)
                .record((long) (durationSeconds * 1_000_000_000), TimeUnit.NANOSECONDS);
    }
}

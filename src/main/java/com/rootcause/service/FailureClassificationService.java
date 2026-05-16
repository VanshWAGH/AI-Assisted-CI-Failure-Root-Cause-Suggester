package com.rootcause.service;

import com.rootcause.classifier.ClassificationResult;
import com.rootcause.classifier.FailureClassifier;
import com.rootcause.domain.CiJob;
import com.rootcause.domain.FailurePattern;
import com.rootcause.domain.JobFailureAnalysis;
import com.rootcause.domain.ModelVersion;
import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.metrics.AnalysisMetrics;
import com.rootcause.repository.CiJobRepository;
import com.rootcause.repository.FailurePatternRepository;
import com.rootcause.repository.JobFailureAnalysisRepository;
import com.rootcause.repository.ModelVersionRepository;
import com.rootcause.web.dto.AnalysisResponse;
import com.rootcause.web.dto.AnalyzeRawRequest;
import com.rootcause.web.dto.AnalyzeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates failure classification:
 *  1. Resolves log content (from jobId lookup or raw payload).
 *  2. Selects classifier(s) based on requested mode.
 *  3. Runs classification.
 *  4. Generates suggestion.
 *  5. Persists analysis result.
 *  6. Records metrics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FailureClassificationService {

    private final List<FailureClassifier> classifiers;
    private final SuggestionGeneratorService suggestionGenerator;
    private final LogIngestionService logIngestionService;
    private final CiJobRepository ciJobRepository;
    private final FailurePatternRepository patternRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final JobFailureAnalysisRepository analysisRepository;
    private final AnalysisMetrics metrics;

    @Value("${rootcause.classifier.default-mode:RULE_BASED}")
    private String defaultMode;

    /**
     * Analyze a job by its internal UUID.
     */
    @Transactional
    public AnalysisResponse analyzeJob(UUID jobId, AnalyzeRequest request) {
        CiJob job = ciJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<String> logLines = logIngestionService.getLogLines(jobId);
        if (logLines.isEmpty()) {
            throw new IllegalStateException("No logs found for job: " + jobId);
        }

        ClassifierMode mode = resolveMode(request != null ? request.getMode() : null);
        return runAnalysis(job, logLines, mode);
    }

    /**
     * Analyze raw log content without a pre-existing job.
     */
    @Transactional
    public AnalysisResponse analyzeRaw(AnalyzeRawRequest request) {
        // Ingest and create job
        CiJob job = logIngestionService.ingestFromRawRequest(request);

        // Resolve log lines
        List<String> logLines;
        if (request.getLogLines() != null && !request.getLogLines().isEmpty()) {
            logLines = request.getLogLines();
        } else if (request.getLogContent() != null) {
            logLines = Arrays.asList(request.getLogContent().split("\n"));
        } else {
            throw new IllegalArgumentException("Either logLines or logContent must be provided");
        }

        ClassifierMode mode = resolveMode(request.getMode());
        return runAnalysis(job, logLines, mode);
    }

    private AnalysisResponse runAnalysis(CiJob job, List<String> logLines, ClassifierMode mode) {
        long startTime = System.nanoTime();

        ClassificationResult result;

        if (mode == ClassifierMode.HYBRID) {
            // Run all available classifiers, pick highest confidence
            result = classifiers.stream()
                    .filter(FailureClassifier::isAvailable)
                    .map(c -> c.classify(logLines))
                    .max(Comparator.comparingDouble(ClassificationResult::getConfidence))
                    .orElse(ClassificationResult.unknown());
            result.setClassifierMode(ClassifierMode.HYBRID);
        } else {
            // Find the classifier for the requested mode
            FailureClassifier classifier = classifiers.stream()
                    .filter(c -> c.supportedMode() == mode && c.isAvailable())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No available classifier for mode: " + mode));
            result = classifier.classify(logLines);
        }

        // Generate suggestion
        SuggestionGeneratorService.Suggestion suggestion = suggestionGenerator.generate(result);

        // Persist analysis
        JobFailureAnalysis analysis = persistAnalysis(job, result, suggestion);

        // Record metrics
        double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        metrics.recordAnalysis(result, durationSeconds);

        log.info("Analysis complete for job {}: type={}, confidence={}, mode={}, duration={}s",
                job.getId(), result.getFailureType(),
                String.format("%.2f", result.getConfidence()), mode,
                String.format("%.3f", durationSeconds));

        return AnalysisResponse.builder()
                .analysisId(analysis.getId())
                .jobId(job.getId())
                .externalJobId(job.getExternalJobId())
                .projectName(job.getProjectName())
                .failureType(result.getFailureType().name().toLowerCase())
                .confidence(result.getConfidence())
                .explanation(suggestion.explanation())
                .suggestedAction(suggestion.suggestedAction())
                .classifierMode(result.getClassifierMode().name().toLowerCase())
                .matchedPatternName(result.getMatchedPatternName())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    private JobFailureAnalysis persistAnalysis(CiJob job, ClassificationResult result,
                                                SuggestionGeneratorService.Suggestion suggestion) {
        FailurePattern pattern = null;
        if (result.getMatchedPatternId() != null) {
            pattern = patternRepository.findById(result.getMatchedPatternId()).orElse(null);
        }

        ModelVersion modelVersion = null;
        if (result.getClassifierMode() == ClassifierMode.ML_ONNX || 
            result.getClassifierMode() == ClassifierMode.HYBRID) {
            modelVersion = modelVersionRepository.findByActiveTrue().orElse(null);
        }

        JobFailureAnalysis analysis = JobFailureAnalysis.builder()
                .job(job)
                .matchedPattern(pattern)
                .modelVersion(modelVersion)
                .failureType(result.getFailureType())
                .confidence(result.getConfidence())
                .explanation(suggestion.explanation())
                .suggestedAction(suggestion.suggestedAction())
                .classifierMode(result.getClassifierMode())
                .matchedText(result.getMatchedText())
                .build();

        return analysisRepository.save(analysis);
    }

    private ClassifierMode resolveMode(String requestedMode) {
        if (requestedMode != null && !requestedMode.isBlank()) {
            try {
                return ClassifierMode.valueOf(requestedMode.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown classifier mode '{}', falling back to default", requestedMode);
            }
        }
        return ClassifierMode.valueOf(defaultMode);
    }
}

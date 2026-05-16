package com.rootcause.service;

import com.rootcause.classifier.ClassificationResult;
import com.rootcause.domain.FailurePattern;
import com.rootcause.repository.FailurePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Renders human-readable explanation and suggested action from a classification result.
 * Uses the matched FailurePattern's templates, filling in {matched_text} and other placeholders.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SuggestionGeneratorService {

    private final FailurePatternRepository patternRepository;

    /**
     * Generate explanation and suggested action for a classification result.
     */
    public Suggestion generate(ClassificationResult result) {
        String explanation;
        String suggestedAction;

        if (result.getMatchedPatternId() != null) {
            // Rule-based: use pattern templates
            FailurePattern pattern = patternRepository.findById(result.getMatchedPatternId())
                    .orElse(null);

            if (pattern != null) {
                explanation = renderTemplate(pattern.getExplanationTemplate(), result);
                suggestedAction = renderTemplate(pattern.getSuggestedActionTemplate(), result);
            } else {
                explanation = buildDefaultExplanation(result);
                suggestedAction = buildDefaultAction(result);
            }
        } else {
            // ML-based or unknown: generate default text
            explanation = buildDefaultExplanation(result);
            suggestedAction = buildDefaultAction(result);
        }

        return new Suggestion(explanation, suggestedAction);
    }

    private String renderTemplate(String template, ClassificationResult result) {
        String rendered = template;
        rendered = rendered.replace("{matched_text}",
                result.getMatchedText() != null ? result.getMatchedText() : "N/A");
        rendered = rendered.replace("{failure_type}",
                result.getFailureType().name().toLowerCase());
        rendered = rendered.replace("{confidence}",
                String.format("%.2f", result.getConfidence()));
        rendered = rendered.replace("{pattern_name}",
                result.getMatchedPatternName() != null ? result.getMatchedPatternName() : "N/A");
        return rendered;
    }

    private String buildDefaultExplanation(ClassificationResult result) {
        return switch (result.getFailureType()) {
            case INFRA -> "Infrastructure-related failure detected. The CI environment encountered a transient issue.";
            case TEST -> "Test failure detected. One or more tests did not pass.";
            case BUILD -> "Build failure detected. The project could not be compiled or packaged.";
            case SECURITY -> "Security issue detected. A vulnerability or policy violation was found.";
            case UNKNOWN -> "Unable to determine the root cause from the available log data.";
        };
    }

    private String buildDefaultAction(ClassificationResult result) {
        return switch (result.getFailureType()) {
            case INFRA -> "Check CI runner health, network connectivity, and resource limits. Retry the pipeline.";
            case TEST -> "Review failing test output and recent code changes. Run tests locally to reproduce.";
            case BUILD -> "Check build configuration, dependency versions, and compiler output.";
            case SECURITY -> "Review the security scan report. Update vulnerable dependencies or fix policy violations.";
            case UNKNOWN -> "Manually inspect the CI logs for error patterns. Consider adding new failure patterns.";
        };
    }

    /**
     * Simple value holder for generated suggestion.
     */
    public record Suggestion(String explanation, String suggestedAction) {}
}

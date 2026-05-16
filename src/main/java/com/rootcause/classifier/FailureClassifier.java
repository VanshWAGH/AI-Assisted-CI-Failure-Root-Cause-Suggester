package com.rootcause.classifier;

import com.rootcause.domain.enums.ClassifierMode;

import java.util.List;

/**
 * Strategy interface for failure classification engines.
 * Implementations: RuleBasedFailureClassifier, OnnxFailureClassifier.
 */
public interface FailureClassifier {

    /**
     * Classify the given log lines and return the best match.
     *
     * @param logLines list of log line strings
     * @return classification result with type, confidence, and matched context
     */
    ClassificationResult classify(List<String> logLines);

    /**
     * Which classifier mode this implementation handles.
     */
    ClassifierMode supportedMode();

    /**
     * Whether this classifier is currently enabled and ready.
     */
    boolean isAvailable();
}

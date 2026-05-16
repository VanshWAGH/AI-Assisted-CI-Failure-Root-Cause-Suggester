package com.rootcause.classifier;

import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.domain.enums.FailureType;
import lombok.*;

import java.util.UUID;

/**
 * Result returned by a FailureClassifier implementation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationResult {

    private FailureType failureType;

    /** Confidence score 0.0 – 1.0 */
    private double confidence;

    /** The matched text fragment from the log (for explainability) */
    private String matchedText;

    /** ID of the FailurePattern that matched (rule-based only, nullable) */
    private UUID matchedPatternId;

    /** Name of the matched pattern (for logging/metrics) */
    private String matchedPatternName;

    /** Which classifier produced this result */
    private ClassifierMode classifierMode;

    /** Create a "no match" result */
    public static ClassificationResult unknown() {
        return ClassificationResult.builder()
                .failureType(FailureType.UNKNOWN)
                .confidence(0.0)
                .matchedText("")
                .classifierMode(ClassifierMode.RULE_BASED)
                .build();
    }
}

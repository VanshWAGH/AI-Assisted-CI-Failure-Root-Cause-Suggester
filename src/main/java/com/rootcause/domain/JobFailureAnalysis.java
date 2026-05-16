package com.rootcause.domain;

import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.domain.enums.FailureType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_failure_analysis", indexes = {
        @Index(name = "idx_analysis_job", columnList = "job_id"),
        @Index(name = "idx_analysis_type", columnList = "failureType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobFailureAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private CiJob job;

    /** The pattern that matched (nullable when ML-only classification) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_pattern_id")
    private FailurePattern matchedPattern;

    /** The model version used (nullable when rule-based only) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_version_id")
    private ModelVersion modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FailureType failureType;

    /** Confidence score 0.0 – 1.0 */
    @Column(nullable = false)
    private Double confidence;

    /** Rendered explanation text */
    @Column(nullable = false, length = 4000)
    private String explanation;

    /** Rendered suggested action */
    @Column(nullable = false, length = 4000)
    private String suggestedAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassifierMode classifierMode;

    /** Raw matched text from the log (for explainability) */
    @Column(length = 2000)
    private String matchedText;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant analyzedAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (analyzedAt == null) analyzedAt = Instant.now();
    }
}

package com.rootcause.domain;

import com.rootcause.domain.enums.FailureType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "failure_pattern", indexes = {
        @Index(name = "idx_failure_pattern_type", columnList = "failureType"),
        @Index(name = "idx_failure_pattern_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailurePattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable label, e.g. "OOM Killer", "Connection Refused" */
    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FailureType failureType;

    /** Java-compatible regex to match against log lines */
    @Column(nullable = false, length = 2000)
    private String regexPattern;

    /**
     * Explanation template with optional {placeholders}.
     * e.g. "Build failed due to {matched_text}."
     */
    @Column(nullable = false, length = 2000)
    private String explanationTemplate;

    /**
     * Suggested action template.
     * e.g. "Check dependency resolution and retry."
     */
    @Column(nullable = false, length = 2000)
    private String suggestedActionTemplate;

    /** Higher priority patterns are preferred when multiple match */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

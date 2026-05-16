package com.rootcause.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ci_job_log", indexes = {
        @Index(name = "idx_ci_job_log_job", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private CiJob job;

    /** Source of the log: console, junit, surefire, gradle, etc. */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String logSource = "console";

    /** Full log content */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String logContent;

    private Integer lineCount;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant ingestedAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (ingestedAt == null) ingestedAt = Instant.now();
        if (logContent != null && lineCount == null) {
            lineCount = logContent.split("\n").length;
        }
    }
}

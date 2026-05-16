package com.rootcause.domain;

import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ci_job", indexes = {
        @Index(name = "idx_ci_job_external", columnList = "externalJobId"),
        @Index(name = "idx_ci_job_project", columnList = "projectName"),
        @Index(name = "idx_ci_job_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String externalJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CiPlatform ciPlatform;

    @Column(nullable = false)
    private String projectName;

    private String pipelineRef;

    private String branchName;

    @Column(length = 64)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    private Instant startedAt;

    private Instant finishedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CiJobLog> logs = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobFailureAnalysis> analyses = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

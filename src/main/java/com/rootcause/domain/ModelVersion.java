package com.rootcause.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "model_version", indexes = {
        @Index(name = "idx_model_version_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String version;

    /** Path to the ONNX model file on disk or in object storage */
    @Column(nullable = false)
    private String modelFilePath;

    /** JSON-encoded feature schema description */
    @Column(length = 4000)
    private String featureSchema;

    /** Training accuracy (0.0 – 1.0) */
    private Double accuracy;

    /** Whether this model version is the currently active one */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;

    private Instant trainedAt;

    private Instant deployedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

package com.rootcause.repository;

import com.rootcause.domain.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, UUID> {

    Optional<ModelVersion> findByActiveTrue();

    Optional<ModelVersion> findByModelNameAndVersion(String modelName, String version);
}

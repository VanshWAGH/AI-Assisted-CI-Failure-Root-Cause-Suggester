package com.rootcause.repository;

import com.rootcause.domain.CiJob;
import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CiJobRepository extends JpaRepository<CiJob, UUID> {

    Optional<CiJob> findByExternalJobIdAndCiPlatform(String externalJobId, CiPlatform ciPlatform);

    List<CiJob> findByProjectNameAndStatus(String projectName, JobStatus status);

    List<CiJob> findByStatusAndCreatedAtAfter(JobStatus status, Instant after);

    boolean existsByExternalJobIdAndCiPlatform(String externalJobId, CiPlatform ciPlatform);
}

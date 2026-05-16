package com.rootcause.repository;

import com.rootcause.domain.JobFailureAnalysis;
import com.rootcause.domain.enums.FailureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobFailureAnalysisRepository extends JpaRepository<JobFailureAnalysis, UUID> {

    @Query("SELECT a FROM JobFailureAnalysis a JOIN FETCH a.job WHERE a.job.id = :jobId ORDER BY a.analyzedAt DESC")
    List<JobFailureAnalysis> findByJobId(UUID jobId);

    List<JobFailureAnalysis> findByFailureType(FailureType failureType);

    @Query("SELECT a.failureType, COUNT(a) FROM JobFailureAnalysis a WHERE a.analyzedAt > :since GROUP BY a.failureType")
    List<Object[]> countByFailureTypeSince(Instant since);

    @Query("SELECT a FROM JobFailureAnalysis a JOIN FETCH a.job WHERE a.job.projectName = :projectName ORDER BY a.analyzedAt DESC")
    List<JobFailureAnalysis> findRecentByProject(String projectName);
}

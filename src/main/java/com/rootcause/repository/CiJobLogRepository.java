package com.rootcause.repository;

import com.rootcause.domain.CiJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CiJobLogRepository extends JpaRepository<CiJobLog, UUID> {

    List<CiJobLog> findByJobId(UUID jobId);

    List<CiJobLog> findByJobIdAndLogSource(UUID jobId, String logSource);
}

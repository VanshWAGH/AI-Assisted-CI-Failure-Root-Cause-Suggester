package com.rootcause.service;

import com.rootcause.domain.CiJob;
import com.rootcause.domain.CiJobLog;
import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.domain.enums.JobStatus;
import com.rootcause.repository.CiJobRepository;
import com.rootcause.repository.CiJobLogRepository;
import com.rootcause.web.dto.AnalyzeRawRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles ingestion of CI job data and log content.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogIngestionService {

    private final CiJobRepository ciJobRepository;
    private final CiJobLogRepository ciJobLogRepository;

    /**
     * Find or create a CiJob from a raw analysis request, store its log, and return the job.
     */
    @Transactional
    public CiJob ingestFromRawRequest(AnalyzeRawRequest request) {
        // Check if job already exists
        CiPlatform platform = parsePlatform(request.getCiPlatform());
        String externalId = request.getPipelineRef() != null ? request.getPipelineRef() : UUID.randomUUID().toString();

        CiJob job = ciJobRepository.findByExternalJobIdAndCiPlatform(externalId, platform)
                .orElseGet(() -> {
                    CiJob newJob = CiJob.builder()
                            .externalJobId(externalId)
                            .ciPlatform(platform)
                            .projectName(request.getProjectName() != null ? request.getProjectName() : "unknown")
                            .pipelineRef(request.getPipelineRef())
                            .branchName(request.getBranchName())
                            .commitSha(request.getCommitSha())
                            .status(JobStatus.FAILED)
                            .build();
                    return ciJobRepository.save(newJob);
                });

        // Store log content
        String logContent = resolveLogContent(request);
        if (logContent != null && !logContent.isBlank()) {
            CiJobLog jobLog = CiJobLog.builder()
                    .job(job)
                    .logSource(request.getLogSource() != null ? request.getLogSource() : "console")
                    .logContent(logContent)
                    .build();
            ciJobLogRepository.save(jobLog);
        }

        log.info("Ingested job {} ({}) with {} chars of log", job.getId(), platform, 
                logContent != null ? logContent.length() : 0);
        return job;
    }

    /**
     * Get all log lines for a job.
     */
    public List<String> getLogLines(UUID jobId) {
        List<CiJobLog> logs = ciJobLogRepository.findByJobId(jobId);
        return logs.stream()
                .flatMap(l -> Arrays.stream(l.getLogContent().split("\n")))
                .toList();
    }

    private String resolveLogContent(AnalyzeRawRequest request) {
        if (request.getLogContent() != null) {
            return request.getLogContent();
        }
        if (request.getLogLines() != null && !request.getLogLines().isEmpty()) {
            return String.join("\n", request.getLogLines());
        }
        return null;
    }

    private CiPlatform parsePlatform(String platform) {
        if (platform == null) return CiPlatform.OTHER;
        try {
            return CiPlatform.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CiPlatform.OTHER;
        }
    }
}

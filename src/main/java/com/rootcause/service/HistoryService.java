package com.rootcause.service;

import com.rootcause.domain.JobFailureAnalysis;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.repository.JobFailureAnalysisRepository;
import com.rootcause.web.dto.AnalysisHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for querying paginated analysis history.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HistoryService {

    private final JobFailureAnalysisRepository analysisRepository;

    /**
     * Return a paginated slice of analyses, optionally filtered by failure type.
     */
    @Transactional(readOnly = true)
    public Page<AnalysisHistoryResponse> getHistory(Pageable pageable, String type) {
        Page<JobFailureAnalysis> page;

        if (type != null && !type.isBlank()) {
            FailureType failureType;
            try {
                failureType = FailureType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unknown failure type '" + type + "'. Valid values: infra, test, build, security, unknown"
                );
            }
            page = analysisRepository.findByFailureType(failureType, pageable);
        } else {
            page = analysisRepository.findAll(pageable);
        }

        return page.map(this::toHistoryResponse);
    }

    private AnalysisHistoryResponse toHistoryResponse(JobFailureAnalysis a) {
        return AnalysisHistoryResponse.builder()
                .analysisId(a.getId())
                .jobId(a.getJob().getId())
                .externalJobId(a.getJob().getExternalJobId())
                .projectName(a.getJob().getProjectName())
                .branchName(a.getJob().getBranchName())
                .failureType(a.getFailureType().name().toLowerCase())
                .confidence(a.getConfidence())
                .classifierMode(a.getClassifierMode().name().toLowerCase())
                .matchedPatternName(
                        a.getMatchedPattern() != null ? a.getMatchedPattern().getName() : null
                )
                .analyzedAt(a.getAnalyzedAt())
                .build();
    }
}

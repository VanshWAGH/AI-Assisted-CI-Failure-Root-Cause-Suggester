package com.rootcause.service;

import com.rootcause.domain.JobFailureAnalysis;
import com.rootcause.repository.JobFailureAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Generates Markdown reports summarizing failure analyses.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisReportService {

    private final JobFailureAnalysisRepository analysisRepository;

    /**
     * Generate a Markdown report for a single job analysis.
     */
    @Transactional(readOnly = true)
    public String generateJobReport(UUID jobId) {
        List<JobFailureAnalysis> analyses = analysisRepository.findByJobId(jobId);
        if (analyses.isEmpty()) {
            return "# No Analysis Found\n\nNo failure analysis found for job `" + jobId + "`.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# CI Failure Root-Cause Analysis Report\n\n");

        for (JobFailureAnalysis a : analyses) {
            sb.append("## Analysis: ").append(a.getId()).append("\n\n");
            sb.append("| Field | Value |\n");
            sb.append("|-------|-------|\n");
            sb.append("| **Job ID** | `").append(a.getJob().getExternalJobId()).append("` |\n");
            sb.append("| **Project** | ").append(a.getJob().getProjectName()).append(" |\n");
            sb.append("| **Branch** | ").append(nullSafe(a.getJob().getBranchName())).append(" |\n");
            sb.append("| **Commit** | `").append(nullSafe(a.getJob().getCommitSha())).append("` |\n");
            sb.append("| **Failure Type** | **").append(a.getFailureType()).append("** |\n");
            sb.append("| **Confidence** | ").append(String.format("%.2f", a.getConfidence())).append(" |\n");
            sb.append("| **Classifier** | ").append(a.getClassifierMode()).append(" |\n");
            sb.append("| **Analyzed At** | ").append(a.getAnalyzedAt()).append(" |\n\n");

            sb.append("### Explanation\n\n").append(a.getExplanation()).append("\n\n");
            sb.append("### Suggested Action\n\n").append(a.getSuggestedAction()).append("\n\n");

            if (a.getMatchedText() != null && !a.getMatchedText().isBlank()) {
                sb.append("### Matched Log Fragment\n\n```\n").append(a.getMatchedText()).append("\n```\n\n");
            }

            sb.append("---\n\n");
        }

        return sb.toString();
    }

    /**
     * Generate a summary report of failure trends for a project.
     */
    @Transactional(readOnly = true)
    public String generateProjectSummary(String projectName) {
        List<JobFailureAnalysis> analyses = analysisRepository.findRecentByProject(projectName);

        StringBuilder sb = new StringBuilder();
        sb.append("# Failure Summary: ").append(projectName).append("\n\n");
        sb.append("Total analyses: **").append(analyses.size()).append("**\n\n");

        if (analyses.isEmpty()) {
            sb.append("No failure analyses found for this project.\n");
            return sb.toString();
        }

        // Count by type
        sb.append("## Breakdown by Failure Type\n\n");
        sb.append("| Type | Count |\n|------|-------|\n");
        analyses.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getFailureType(),
                        java.util.stream.Collectors.counting()))
                .forEach((type, count) -> sb.append("| ").append(type).append(" | ").append(count).append(" |\n"));

        sb.append("\n## Recent Failures\n\n");
        analyses.stream().limit(10).forEach(a -> {
            sb.append("- **").append(a.getFailureType()).append("** (")
                    .append(String.format("%.0f%%", a.getConfidence() * 100)).append(") — ")
                    .append(a.getExplanation(), 0, Math.min(a.getExplanation().length(), 100))
                    .append("\n");
        });

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}

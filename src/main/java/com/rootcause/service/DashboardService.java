package com.rootcause.service;

import com.rootcause.domain.JobFailureAnalysis;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.repository.JobFailureAnalysisRepository;
import com.rootcause.web.dto.DashboardStatsDTO;
import com.rootcause.web.dto.RecentAnalysisDTO;
import com.rootcause.web.dto.TrendDayDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final JobFailureAnalysisRepository analysisRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Object[]> counts = analysisRepository.countByFailureTypeSince(since);

        long total = 0;
        long infra = 0;
        long test = 0;
        long build = 0;
        long security = 0;
        long unknown = 0;

        for (Object[] row : counts) {
            FailureType type = (FailureType) row[0];
            long count = (long) row[1];
            total += count;

            switch (type) {
                case INFRA -> infra = count;
                case TEST -> test = count;
                case BUILD -> build = count;
                case SECURITY -> security = count;
                case UNKNOWN -> unknown = count;
            }
        }

        Double avgConfidenceVal = analysisRepository.getAverageConfidenceSince(since);
        double avgConfidence = avgConfidenceVal != null ? avgConfidenceVal : 0.0;

        return DashboardStatsDTO.builder()
                .totalFailuresToday(total)
                .infraFailures(infra)
                .testFailures(test)
                .buildFailures(build)
                .securityFailures(security)
                .unknownFailures(unknown)
                .avgConfidence(avgConfidence)
                .lastUpdated(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrendDayDTO> getDashboardTrend(int days) {
        Instant since = LocalDate.now().minusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<JobFailureAnalysis> analyses = analysisRepository.findAnalysesSince(since);

        Map<LocalDate, TrendDayDTO> trendMap = new LinkedHashMap<>();
        for (int i = days; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            trendMap.put(date, TrendDayDTO.builder()
                    .date(date.toString())
                    .infra(0)
                    .test(0)
                    .build(0)
                    .security(0)
                    .unknown(0)
                    .build());
        }

        for (JobFailureAnalysis a : analyses) {
            LocalDate analysisDate = LocalDate.ofInstant(a.getAnalyzedAt(), ZoneOffset.UTC);
            TrendDayDTO dto = trendMap.get(analysisDate);
            if (dto != null) {
                switch (a.getFailureType()) {
                    case INFRA -> dto.setInfra(dto.getInfra() + 1);
                    case TEST -> dto.setTest(dto.getTest() + 1);
                    case BUILD -> dto.setBuild(dto.getBuild() + 1);
                    case SECURITY -> dto.setSecurity(dto.getSecurity() + 1);
                    case UNKNOWN -> dto.setUnknown(dto.getUnknown() + 1);
                }
            }
        }

        return new ArrayList<>(trendMap.values());
    }

    @Transactional(readOnly = true)
    public List<RecentAnalysisDTO> getRecentAnalyses(int limit) {
        List<JobFailureAnalysis> analyses = analysisRepository.findRecentAnalyses(PageRequest.of(0, limit));
        List<RecentAnalysisDTO> result = new ArrayList<>();

        for (JobFailureAnalysis a : analyses) {
            result.add(RecentAnalysisDTO.builder()
                    .analysisId(a.getId())
                    .jobId(a.getJob().getId())
                    .externalJobId(a.getJob().getExternalJobId())
                    .projectName(a.getJob().getProjectName())
                    .branchName(a.getJob().getBranchName())
                    .failureType(a.getFailureType().name().toLowerCase())
                    .confidence(a.getConfidence())
                    .classifierMode(a.getClassifierMode().name().toLowerCase())
                    .matchedPatternName(a.getMatchedPattern() != null ? a.getMatchedPattern().getName() : null)
                    .explanation(a.getExplanation())
                    .suggestedAction(a.getSuggestedAction())
                    .analyzedAt(a.getAnalyzedAt())
                    .build());
        }

        return result;
    }
}

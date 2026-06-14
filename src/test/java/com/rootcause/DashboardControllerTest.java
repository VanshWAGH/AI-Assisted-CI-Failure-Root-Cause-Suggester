package com.rootcause;

import com.rootcause.domain.CiJob;
import com.rootcause.domain.JobFailureAnalysis;
import com.rootcause.domain.enums.CiPlatform;
import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.domain.enums.JobStatus;
import com.rootcause.repository.CiJobRepository;
import com.rootcause.repository.JobFailureAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Dashboard Controller Integration Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CiJobRepository ciJobRepository;

    @Autowired
    private JobFailureAnalysisRepository analysisRepository;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        ciJobRepository.deleteAll();

        // Seed some job failure analyses
        CiJob job1 = Objects.requireNonNull(ciJobRepository.save(CiJob.builder()
                .externalJobId("job-stats-1")
                .ciPlatform(CiPlatform.GITLAB)
                .projectName("my-org/my-project")
                .branchName("main")
                .status(JobStatus.FAILED)
                .build()));

        analysisRepository.save(JobFailureAnalysis.builder()
                .job(job1)
                .failureType(FailureType.INFRA)
                .confidence(0.95)
                .explanation("Infra connection error")
                .suggestedAction("Check firewall settings.")
                .classifierMode(ClassifierMode.RULE_BASED)
                .analyzedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build());

        CiJob job2 = Objects.requireNonNull(ciJobRepository.save(CiJob.builder()
                .externalJobId("job-stats-2")
                .ciPlatform(CiPlatform.JENKINS)
                .projectName("my-org/my-project")
                .branchName("dev")
                .status(JobStatus.FAILED)
                .build()));

        analysisRepository.save(JobFailureAnalysis.builder()
                .job(job2)
                .failureType(FailureType.TEST)
                .confidence(0.85)
                .explanation("NullPointerException in tests")
                .suggestedAction("Fix test code.")
                .classifierMode(ClassifierMode.RULE_BASED)
                .analyzedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats should return aggregate failure stats")
    void shouldReturnDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFailuresToday", is(2)))
                .andExpect(jsonPath("$.infraFailures", is(1)))
                .andExpect(jsonPath("$.testFailures", is(1)))
                .andExpect(jsonPath("$.avgConfidence", closeTo(0.90, 0.01)))
                .andExpect(jsonPath("$.lastUpdated", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/trend should return daily trend statistics")
    void shouldReturnDashboardTrend() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/trend?days=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8))) // 7 days in past + today = 8 items
                // Both analyses happened today, so last entry should have infra=1 and test=1
                .andExpect(jsonPath("$[7].date", notNullValue()))
                .andExpect(jsonPath("$[7].infra", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[7].test", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/recent should return list of recent failure analyses")
    void shouldReturnRecentAnalyses() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/recent?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].externalJobId", is("job-stats-2")))
                .andExpect(jsonPath("$[0].failureType", is("test")))
                .andExpect(jsonPath("$[1].externalJobId", is("job-stats-1")))
                .andExpect(jsonPath("$[1].failureType", is("infra")));
    }
}

package com.rootcause;

import com.rootcause.classifier.ClassificationResult;
import com.rootcause.classifier.RuleBasedFailureClassifier;
import com.rootcause.domain.FailurePattern;
import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.repository.FailurePatternRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Rule-Based Failure Classifier Tests")
class RuleBasedFailureClassifierTest {

    @Autowired
    private FailurePatternRepository patternRepository;

    private RuleBasedFailureClassifier classifier;

    @BeforeEach
    void setUp() {
        // Seed test patterns
        patternRepository.deleteAll();

        patternRepository.save(FailurePattern.builder()
                .name("Connection Refused")
                .failureType(FailureType.INFRA)
                .regexPattern("(?i)(connection\\s+refused|ECONNREFUSED)")
                .explanationTemplate("Connection refused: {matched_text}")
                .suggestedActionTemplate("Check target service and retry.")
                .priority(90)
                .build());

        patternRepository.save(FailurePattern.builder()
                .name("OOM Killed")
                .failureType(FailureType.INFRA)
                .regexPattern("(?i)(OutOfMemoryError|OOM\\s*Kill)")
                .explanationTemplate("Out of memory: {matched_text}")
                .suggestedActionTemplate("Increase memory limits.")
                .priority(95)
                .build());

        patternRepository.save(FailurePattern.builder()
                .name("JUnit Failure")
                .failureType(FailureType.TEST)
                .regexPattern("(?i)(Tests?\\s+run:.*Failures?:\\s*[1-9])")
                .explanationTemplate("Test failures: {matched_text}")
                .suggestedActionTemplate("Review failing tests.")
                .priority(70)
                .build());

        patternRepository.save(FailurePattern.builder()
                .name("Gradle Build Failure")
                .failureType(FailureType.BUILD)
                .regexPattern("(?i)(BUILD FAILED|Execution failed for task)")
                .explanationTemplate("Build failed: {matched_text}")
                .suggestedActionTemplate("Check build configuration.")
                .priority(80)
                .build());

        patternRepository.save(FailurePattern.builder()
                .name("CVE Vulnerability")
                .failureType(FailureType.SECURITY)
                .regexPattern("(?i)(CVE-\\d{4}-\\d+|vulnerability\\s+found)")
                .explanationTemplate("Vulnerability: {matched_text}")
                .suggestedActionTemplate("Update vulnerable dependency.")
                .priority(90)
                .build());

        classifier = new RuleBasedFailureClassifier(patternRepository);
        classifier.loadPatterns();
    }

    @Test
    @DisplayName("Should classify connection refused as INFRA")
    void shouldClassifyConnectionRefused() {
        List<String> logLines = List.of(
                "[INFO] Starting application...",
                "[ERROR] java.net.ConnectException: Connection refused (Connection refused)",
                "[ERROR] Failed to connect to database at localhost:5432"
        );

        ClassificationResult result = classifier.classify(logLines);

        assertEquals(FailureType.INFRA, result.getFailureType());
        assertTrue(result.getConfidence() > 0.5);
        assertEquals(ClassifierMode.RULE_BASED, result.getClassifierMode());
        assertNotNull(result.getMatchedText());
        assertNotNull(result.getMatchedPatternId());
    }

    @Test
    @DisplayName("Should classify OOM as INFRA with higher priority than connection refused")
    void shouldClassifyOomWithHighPriority() {
        List<String> logLines = List.of(
                "[ERROR] java.lang.OutOfMemoryError: Java heap space",
                "[ERROR] Connection refused while retrying"
        );

        ClassificationResult result = classifier.classify(logLines);

        // OOM has priority 95 vs connection refused 90
        assertEquals(FailureType.INFRA, result.getFailureType());
        assertEquals("OOM Killed", result.getMatchedPatternName());
    }

    @Test
    @DisplayName("Should classify JUnit test failures as TEST")
    void shouldClassifyTestFailures() {
        List<String> logLines = List.of(
                "[INFO] Running com.example.MyServiceTest",
                "Tests run: 15, Failures: 3, Errors: 0, Skipped: 1",
                "[INFO] BUILD SUCCESS"
        );

        ClassificationResult result = classifier.classify(logLines);

        assertEquals(FailureType.TEST, result.getFailureType());
        assertTrue(result.getConfidence() > 0.0);
    }

    @Test
    @DisplayName("Should classify build failures as BUILD")
    void shouldClassifyBuildFailure() {
        List<String> logLines = List.of(
                "> Task :compileJava FAILED",
                "Execution failed for task ':compileJava'",
                "BUILD FAILED in 30s"
        );

        ClassificationResult result = classifier.classify(logLines);

        assertEquals(FailureType.BUILD, result.getFailureType());
    }

    @Test
    @DisplayName("Should classify CVE as SECURITY")
    void shouldClassifyCveAsSecurity() {
        List<String> logLines = List.of(
                "[WARN] Dependency check found issues",
                "CVE-2024-12345: Critical vulnerability found in log4j-core:2.14.1",
                "Build will fail due to security policy"
        );

        ClassificationResult result = classifier.classify(logLines);

        assertEquals(FailureType.SECURITY, result.getFailureType());
    }

    @Test
    @DisplayName("Should return UNKNOWN for unmatched logs")
    void shouldReturnUnknownForNoMatch() {
        List<String> logLines = List.of(
                "[INFO] Everything is fine",
                "[INFO] Build completed successfully",
                "[INFO] All tests passed"
        );

        ClassificationResult result = classifier.classify(logLines);

        assertEquals(FailureType.UNKNOWN, result.getFailureType());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    @DisplayName("Should return UNKNOWN for null/empty input")
    void shouldHandleNullInput() {
        assertEquals(FailureType.UNKNOWN, classifier.classify(null).getFailureType());
        assertEquals(FailureType.UNKNOWN, classifier.classify(List.of()).getFailureType());
    }

    @Test
    @DisplayName("Should report as available when patterns are loaded")
    void shouldBeAvailable() {
        assertTrue(classifier.isAvailable());
        assertEquals(ClassifierMode.RULE_BASED, classifier.supportedMode());
    }
}

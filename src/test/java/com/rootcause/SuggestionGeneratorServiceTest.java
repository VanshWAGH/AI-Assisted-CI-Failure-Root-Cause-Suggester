package com.rootcause;

import com.rootcause.service.SuggestionGeneratorService;
import com.rootcause.classifier.ClassificationResult;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Suggestion Generator Service Tests")
class SuggestionGeneratorServiceTest {

    @Autowired private SuggestionGeneratorService suggestionGenerator;
    @Autowired private FailurePatternRepository patternRepository;

    private FailurePattern savedPattern;

    @BeforeEach
    void setUp() {
        patternRepository.deleteAll();
        savedPattern = patternRepository.save(FailurePattern.builder()
                .name("OOM Test")
                .failureType(FailureType.INFRA)
                .regexPattern("(?i)(OutOfMemoryError)")
                .explanationTemplate("Out of memory detected: {matched_text}. Confidence: {confidence}.")
                .suggestedActionTemplate("Increase memory. Pattern: {pattern_name}.")
                .priority(95)
                .build());
    }

    @Test
    @DisplayName("Should render template placeholders from matched pattern")
    void shouldRenderTemplateWithPlaceholders() {
        ClassificationResult result = ClassificationResult.builder()
                .failureType(FailureType.INFRA)
                .confidence(0.92)
                .matchedText("java.lang.OutOfMemoryError: Java heap space")
                .matchedPatternId(savedPattern.getId())
                .matchedPatternName("OOM Test")
                .classifierMode(ClassifierMode.RULE_BASED)
                .build();

        SuggestionGeneratorService.Suggestion suggestion = suggestionGenerator.generate(result);

        assertTrue(suggestion.explanation().contains("OutOfMemoryError"));
        assertTrue(suggestion.explanation().contains("0.92"));
        assertTrue(suggestion.suggestedAction().contains("OOM Test"));
    }

    @Test
    @DisplayName("Should generate default suggestion for ML results without pattern")
    void shouldGenerateDefaultForMl() {
        ClassificationResult result = ClassificationResult.builder()
                .failureType(FailureType.TEST)
                .confidence(0.78)
                .matchedText("Test failure detected")
                .classifierMode(ClassifierMode.ML_ONNX)
                .build();

        SuggestionGeneratorService.Suggestion suggestion = suggestionGenerator.generate(result);

        assertNotNull(suggestion.explanation());
        assertNotNull(suggestion.suggestedAction());
        assertTrue(suggestion.explanation().contains("Test failure"));
    }

    @Test
    @DisplayName("Should handle UNKNOWN failure type")
    void shouldHandleUnknownType() {
        ClassificationResult result = ClassificationResult.unknown();

        SuggestionGeneratorService.Suggestion suggestion = suggestionGenerator.generate(result);

        assertTrue(suggestion.explanation().contains("Unable to determine"));
        assertTrue(suggestion.suggestedAction().contains("Manually inspect"));
    }
}

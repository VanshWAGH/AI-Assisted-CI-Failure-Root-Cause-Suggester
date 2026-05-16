package com.rootcause.classifier;

import com.rootcause.domain.FailurePattern;
import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.domain.enums.FailureType;
import com.rootcause.repository.FailurePatternRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Rule-based failure classifier that matches log lines against regex patterns
 * stored in the database.
 *
 * Strategy:
 *  1. Load all active patterns (sorted by priority desc).
 *  2. For each log line, test against each pattern.
 *  3. Collect all matches with their priority as confidence proxy.
 *  4. Pick the match with the highest priority (highest confidence).
 *  5. If multiple types match, use weighted scoring to pick dominant type.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RuleBasedFailureClassifier implements FailureClassifier {

    private final FailurePatternRepository patternRepository;

    /** Cached compiled patterns (refreshed on startup and via reload) */
    private List<CompiledPattern> compiledPatterns = new ArrayList<>();

    @PostConstruct
    public void loadPatterns() {
        List<FailurePattern> patterns = patternRepository.findByActiveTrueOrderByPriorityDesc();
        compiledPatterns = patterns.stream()
                .map(fp -> new CompiledPattern(
                        fp.getId(),
                        fp.getName(),
                        fp.getFailureType(),
                        Pattern.compile(fp.getRegexPattern(), Pattern.MULTILINE),
                        fp.getPriority()
                ))
                .collect(Collectors.toList());
        log.info("Loaded {} active failure patterns for rule-based classification", compiledPatterns.size());
    }

    /** Reload patterns from DB (called on-demand or via endpoint) */
    public void reloadPatterns() {
        loadPatterns();
    }

    @Override
    public ClassificationResult classify(List<String> logLines) {
        if (logLines == null || logLines.isEmpty()) {
            return ClassificationResult.unknown();
        }

        String fullLog = String.join("\n", logLines);
        List<PatternMatch> allMatches = new ArrayList<>();

        for (CompiledPattern cp : compiledPatterns) {
            Matcher matcher = cp.pattern().matcher(fullLog);
            if (matcher.find()) {
                String matched = matcher.group().length() > 200
                        ? matcher.group().substring(0, 200) + "..."
                        : matcher.group();
                allMatches.add(new PatternMatch(cp, matched));
            }
        }

        if (allMatches.isEmpty()) {
            log.debug("No patterns matched for log ({} lines)", logLines.size());
            return ClassificationResult.unknown();
        }

        // Pick the highest-priority match
        PatternMatch best = allMatches.get(0); // already sorted by priority (desc)

        // Calculate confidence: base on priority and number of matches for same type
        long sameTypeCount = allMatches.stream()
                .filter(m -> m.compiledPattern().failureType() == best.compiledPattern().failureType())
                .count();

        // Confidence formula: priority-based (0.5–1.0) boosted by multiple matches
        double baseConfidence = Math.min(best.compiledPattern().priority() / 100.0, 1.0);
        double boost = Math.min((sameTypeCount - 1) * 0.05, 0.2);
        double confidence = Math.min(baseConfidence + boost, 1.0);

        log.info("Rule-based classification: type={}, confidence={}, pattern={}, matches={}",
                best.compiledPattern().failureType(), String.format("%.2f", confidence), best.compiledPattern().name(), allMatches.size());

        return ClassificationResult.builder()
                .failureType(best.compiledPattern().failureType())
                .confidence(confidence)
                .matchedText(best.matchedText())
                .matchedPatternId(best.compiledPattern().id())
                .matchedPatternName(best.compiledPattern().name())
                .classifierMode(ClassifierMode.RULE_BASED)
                .build();
    }

    @Override
    public ClassifierMode supportedMode() {
        return ClassifierMode.RULE_BASED;
    }

    @Override
    public boolean isAvailable() {
        return !compiledPatterns.isEmpty();
    }

    // ── Inner records ──────────────────────────────────────────────

    private record CompiledPattern(
            UUID id,
            String name,
            FailureType failureType,
            Pattern pattern,
            int priority
    ) {}

    private record PatternMatch(
            CompiledPattern compiledPattern,
            String matchedText
    ) {}
}

package com.rootcause.classifier;

import ai.onnxruntime.*;
import com.rootcause.domain.enums.ClassifierMode;
import com.rootcause.domain.enums.FailureType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ONNX-based failure classifier using a pre-trained text classification model.
 *
 * The model expects TF-IDF-like features as input:
 *   - Input shape: [1, vocab_size] (float32)
 *   - Output shape: [1, num_classes] (float32 probabilities)
 *
 * Feature extraction:
 *   Uses a simple bag-of-words approach with predefined keywords per category.
 *   In production, this would use the same TF-IDF vectorizer used during training.
 */
@Component
@ConditionalOnProperty(name = "rootcause.classifier.onnx.enabled", havingValue = "true")
@Slf4j
public class OnnxFailureClassifier implements FailureClassifier {

    private final OrtEnvironment environment;
    private final OrtSession session;

    /** Ordered class labels matching model output indices */
    private static final FailureType[] CLASS_LABELS = {
            FailureType.INFRA,
            FailureType.TEST,
            FailureType.BUILD,
            FailureType.SECURITY,
            FailureType.UNKNOWN
    };

    /**
     * Keyword features used for bag-of-words vectorization.
     * In production, load from the same vocabulary used during model training.
     */
    private static final List<String> VOCABULARY = List.of(
            // Infra keywords
            "connection", "refused", "timeout", "oom", "outofmemory", "memory", "disk", "space",
            "dns", "network", "docker", "daemon", "socket", "ssl", "certificate", "runner",
            "pool", "hikari", "jdbc", "unreachable", "killed", "econnrefused", "enospc",
            // Test keywords
            "test", "junit", "assertion", "assertionerror", "failed", "failure", "testcase",
            "nullpointerexception", "mockbean", "applicationcontext", "flaky", "surefire",
            // Build keywords
            "build", "compile", "compilation", "gradle", "maven", "dependency", "resolve",
            "artifact", "javac", "checkstyle", "lint", "pmd", "spotbugs",
            // Security keywords
            "cve", "vulnerability", "security", "scan", "trivy", "grype", "owasp",
            "secret", "credential", "leak", "gitleaks", "password", "apikey"
    );

    public OnnxFailureClassifier(OrtEnvironment environment, OrtSession session) {
        this.environment = environment;
        this.session = session;
        if (session != null) {
            log.info("ONNX Failure Classifier initialized with {} vocabulary features", VOCABULARY.size());
        }
    }

    @Override
    public ClassificationResult classify(List<String> logLines) {
        if (session == null || logLines == null || logLines.isEmpty()) {
            return ClassificationResult.unknown();
        }

        try {
            // Extract features
            float[] features = extractFeatures(logLines);

            // Run inference
            float[] probabilities = runInference(features);

            // Find best class
            int bestIdx = 0;
            float bestProb = probabilities[0];
            for (int i = 1; i < probabilities.length && i < CLASS_LABELS.length; i++) {
                if (probabilities[i] > bestProb) {
                    bestProb = probabilities[i];
                    bestIdx = i;
                }
            }

            FailureType predictedType = CLASS_LABELS[bestIdx];
            double confidence = bestProb;

            // Find a representative matched line for explainability
            String matchedText = findRepresentativeLine(logLines, predictedType);

            log.info("ONNX classification: type={}, confidence={}", predictedType, String.format("%.4f", confidence));

            return ClassificationResult.builder()
                    .failureType(predictedType)
                    .confidence(confidence)
                    .matchedText(matchedText)
                    .classifierMode(ClassifierMode.ML_ONNX)
                    .build();

        } catch (OrtException e) {
            log.error("ONNX inference failed: {}", e.getMessage(), e);
            return ClassificationResult.unknown();
        }
    }

    @Override
    public ClassifierMode supportedMode() {
        return ClassifierMode.ML_ONNX;
    }

    @Override
    public boolean isAvailable() {
        return session != null;
    }

    /**
     * Extract bag-of-words features from log lines.
     */
    private float[] extractFeatures(List<String> logLines) {
        String fullText = String.join(" ", logLines).toLowerCase();
        float[] features = new float[VOCABULARY.size()];

        for (int i = 0; i < VOCABULARY.size(); i++) {
            String keyword = VOCABULARY.get(i);
            // Count occurrences (simple TF)
            int count = countOccurrences(fullText, keyword);
            // Log-normalize
            features[i] = count > 0 ? (float) (1.0 + Math.log(count)) : 0.0f;
        }

        return features;
    }

    private int countOccurrences(String text, String keyword) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Run ONNX model inference.
     */
    private float[] runInference(float[] features) throws OrtException {
        long[] shape = {1, features.length};
        OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(features), shape);

        try (OrtSession.Result result = session.run(
                Collections.singletonMap(session.getInputNames().iterator().next(), inputTensor))) {

            float[][] output = (float[][]) result.get(0).getValue();
            float[] probs = output[0];

            // Apply softmax if raw logits
            if (!isSoftmaxed(probs)) {
                probs = softmax(probs);
            }

            return probs;
        }
    }

    private boolean isSoftmaxed(float[] values) {
        float sum = 0;
        for (float v : values) {
            if (v < 0) return false;
            sum += v;
        }
        return Math.abs(sum - 1.0) < 0.01;
    }

    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) max = Math.max(max, v);

        float sum = 0;
        float[] result = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            result[i] = (float) Math.exp(logits[i] - max);
            sum += result[i];
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= sum;
        }
        return result;
    }

    /**
     * Find a representative log line for the predicted failure type (for explainability).
     */
    private String findRepresentativeLine(List<String> logLines, FailureType type) {
        Map<FailureType, List<String>> typeKeywords = Map.of(
                FailureType.INFRA, List.of("connection", "timeout", "oom", "memory", "disk", "docker", "dns"),
                FailureType.TEST, List.of("test", "assert", "junit", "failure", "null"),
                FailureType.BUILD, List.of("build", "compile", "gradle", "maven", "dependency"),
                FailureType.SECURITY, List.of("cve", "vulnerability", "security", "secret", "scan")
        );

        List<String> keywords = typeKeywords.getOrDefault(type, List.of("error", "failed"));

        return logLines.stream()
                .filter(line -> {
                    String lower = line.toLowerCase();
                    return keywords.stream().anyMatch(lower::contains);
                })
                .findFirst()
                .map(line -> line.length() > 200 ? line.substring(0, 200) + "..." : line)
                .orElse("ML-classified based on overall log pattern");
    }
}

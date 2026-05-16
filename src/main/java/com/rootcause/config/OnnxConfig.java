package com.rootcause.config;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ONNX Runtime configuration.
 * Only activated when rootcause.classifier.onnx.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "rootcause.classifier.onnx.enabled", havingValue = "true")
@Slf4j
public class OnnxConfig {

    @Value("${rootcause.classifier.onnx.model-path:models/log-classifier.onnx}")
    private String modelPath;

    @Bean(destroyMethod = "close")
    public OrtEnvironment ortEnvironment() {
        log.info("Initializing ONNX Runtime environment");
        return OrtEnvironment.getEnvironment();
    }

    @Bean(destroyMethod = "close")
    public OrtSession ortSession(OrtEnvironment env) throws OrtException {
        Path path = Path.of(modelPath);
        if (!Files.exists(path)) {
            log.warn("ONNX model file not found at {}. ML classification will be unavailable.", modelPath);
            return null;
        }

        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
        opts.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());

        log.info("Loading ONNX model from: {}", modelPath);
        return env.createSession(modelPath, opts);
    }
}

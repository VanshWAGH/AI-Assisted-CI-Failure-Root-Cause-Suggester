#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os, sys
os.environ.setdefault("PYTHONIOENCODING", "utf-8")
# Reconfigure stdout/stderr to utf-8 on Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
"""
ML Training Pipeline for CI Failure Log Classification.
Phase 2 of the AI-Assisted CI-Failure Root-Cause Suggester.

Pipeline:
  1. Load or generate training data (CSV: log_text, failure_type)
  2. TF-IDF vectorization (unigrams + bigrams)
  3. Train LightGBM multiclass classifier
  4. Evaluate: classification report + cross-validation
  5. Export model to ONNX format via onnxmltools
  6. Save vocabulary JSON + metadata JSON (consumed by Java OnnxFailureClassifier)

Usage:
  # Install deps:
  pip install -r requirements.txt

  # Generate synthetic data + train + export:
  python train.py --generate-data --samples 1000 --output ../models/log-classifier.onnx

  # Train on your own CSV:
  python train.py --data training_data.csv --output ../models/log-classifier.onnx

  # More features:
  python train.py --generate-data --samples 2000 --max-features 1000 --output ../models/log-classifier.onnx
"""

import argparse
import json
import os
import sys
import random
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import lightgbm as lgb

# Suppress non-critical warnings from older APIs
warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", category=UserWarning)

# ── Constants ──────────────────────────────────────────────────────────────────

FAILURE_TYPES = ["infra", "test", "build", "security", "unknown"]
LABEL_MAP = {label: idx for idx, label in enumerate(FAILURE_TYPES)}
INV_LABEL_MAP = {idx: label for label, idx in LABEL_MAP.items()}

# ── Data Loading ───────────────────────────────────────────────────────────────

def load_data(data_path: str) -> pd.DataFrame:
    """Load training data from CSV file."""
    path = Path(data_path)
    if not path.exists():
        raise FileNotFoundError(f"Training data not found: {data_path}")

    df = pd.read_csv(data_path)
    required_cols = {"log_text", "failure_type"}
    if not required_cols.issubset(df.columns):
        raise ValueError(f"CSV must have columns: {required_cols}. Found: {set(df.columns)}")

    df["failure_type"] = df["failure_type"].str.lower().str.strip()
    before = len(df)
    df = df[df["failure_type"].isin(FAILURE_TYPES)].dropna(subset=["log_text"])
    after = len(df)

    if before != after:
        print(f"  Dropped {before - after} rows with unknown labels or missing text.")

    print(f"Loaded {len(df)} samples")
    print(f"Class distribution:\n{df['failure_type'].value_counts()}\n")
    return df


# ── Synthetic Data Generation ──────────────────────────────────────────────────

def generate_sample_data(output_path: str, n_samples: int = 1000) -> pd.DataFrame:
    """Generate rich synthetic training data covering all failure categories."""

    templates = {
        "infra": [
            "java.net.ConnectException: Connection refused (Connection refused)\n  at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)",
            "java.lang.OutOfMemoryError: Java heap space\n  at java.util.Arrays.copyOf(Arrays.java:3210)",
            "ERROR: No space left on device (errno=28)\nfailed to write output file",
            "Unable to acquire JDBC Connection: HikariPool-1 - Connection is not available, request timed out after 30001ms",
            "SSL handshake failed: PKIX path building failed: unable to find valid certification path",
            "Could not resolve host: registry.example.com: Name or service not known",
            "Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?",
            "Job exceeded maximum allowed timeout of 3600 seconds",
            "FATAL: Connection to database failed: connection refused",
            "com.zaxxer.hikari.pool.HikariPool: HikariPool-1 - Connection is not available, timeout after 30000ms",
            "Error response from daemon: OOM killer killed your process",
            "fatal error: OOM at gc/mark_termination.go:1234",
            "curl: (6) Could not resolve host: maven.example.com",
            "Error: ENOSPC: no space left on device, write",
            "docker: Error response from daemon: Conflict. The container name is already in use.",
        ],
        "test": [
            "Tests run: 45, Failures: 3, Errors: 0, Skipped: 2\n  FAILED com.example.UserServiceTest.testCreateUser",
            "java.lang.AssertionError: expected:<200> but was:<500>\n  at org.junit.Assert.fail(Assert.java:88)",
            "java.lang.NullPointerException\n  at com.example.service.UserServiceTest.testGetUser(UserServiceTest.java:42)",
            "org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'userService'",
            "Failed to load ApplicationContext for test class com.example.IntegrationTest",
            "TestTimedOutException: test timed out after 30000 milliseconds",
            "org.junit.ComparisonFailure: expected:<Hello [World]> but was:<Hello [there]>",
            "java.lang.AssertionError: expected true but was false\n  at TestFoo.testBar(TestFoo.java:56)",
            "FAILED: com.example.api.UserControllerTest - 2 tests FAILED",
            "ERROR in test setup: @Before method threw exception",
            "Mockito: Wanted but not invoked: mockUserRepo.save()",
            "org.awaitility.core.ConditionTimeoutException: Condition not met within 5 seconds",
            "[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0",
            "AssertionError: 404 != 200",
            "junit.framework.AssertionFailedError: Expected 5 but was 3",
        ],
        "build": [
            "FAILURE: Build failed with an exception.\n* What went wrong:\nExecution failed for task ':compileJava'.",
            "error: cannot find symbol\n  class UserRepository\n  location: package com.example.repository",
            "Could not resolve all dependencies for configuration ':compileClasspath'.\n> Could not find com.example:missing-lib:1.0",
            "[ERROR] BUILD FAILURE\n[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11:compile",
            "Checkstyle rule violated: MagicNumber at line 42, column 18.",
            "SpotBugs found 3 bugs in com.example.UserService",
            "PMD: Avoid unused private fields such as 'logger'.",
            "> Task :test FAILED\nCould not resolve com.h2database:h2:2.2.224",
            "error: package jakarta.validation does not exist",
            "Compilation failed; see the compiler error output for details.\njavac: error: invalid flag: --enable-preview",
            "[ERROR] Source option 8 is no longer supported. Use 11 or later.",
            "error: method does not override or implement a method from a supertype\n  @Override",
            "Could not find artifact org.example:lib:jar:2.0.0 in central",
            "Gradle build daemon disappeared unexpectedly",
            "BUILD FAILED in 42s\n3 actionable tasks: 1 executed, 2 failed",
        ],
        "security": [
            "CVE-2024-12345: Critical vulnerability found in log4j-core:2.14.1 (CVSS 9.8)",
            "Secret detected: AWS access key found in config/application.properties",
            "Trivy scan found 3 CRITICAL and 7 HIGH vulnerabilities in openjdk:17-alpine base image",
            "OWASP dependency-check failed: 5 vulnerabilities exceed configured threshold (CVSS >= 7.0)",
            "gitleaks: possible secret in commit abc123: password = 'supersecret123'",
            "Snyk test failed: 2 critical severity vulnerabilities found in spring-core:5.3.20",
            "Security policy violation: image uses root user. Add USER nonroot to Dockerfile.",
            "CRITICAL: jackson-databind 2.13.0 - Remote Code Execution via CVE-2022-42003",
            "dependency-check: FAIL - 3 dependencies with CVSS score >= 8.0",
            "SonarQube Security Hotspot: SQL Injection vulnerability in UserDao.java:123",
        ],
        "unknown": [
            "Process exited with code 1\nThe process '/bin/bash' exited with code 1",
            "An unexpected error occurred during pipeline execution",
            "Script terminated with non-zero exit status",
            "error: exit status 2",
            "The runner has disconnected unexpectedly",
        ],
    }

    weights = [30, 30, 25, 10, 5]
    rows = []

    noise_pool = [
        "[INFO] Building project...",
        "[DEBUG] Loading configuration...",
        "Downloading dependencies...",
        "Running CI pipeline step {step}...",
        "[INFO] Starting Spring Boot application...",
        "Executing Gradle task...",
        "Setting up test environment...",
        "[WARN] Deprecated API usage detected",
        "Connecting to external service...",
        "Uploading artifacts to registry...",
    ]

    for _ in range(n_samples):
        failure_type = random.choices(FAILURE_TYPES, weights=weights, k=1)[0]
        base_log = random.choice(templates[failure_type])

        # Add 2-4 noise lines before/after the key error
        n_noise = random.randint(2, 4)
        noise_lines = [
            random.choice(noise_pool).replace("{step}", str(random.randint(1, 20)))
            for _ in range(n_noise)
        ]

        # Randomly insert base log at a random position
        pos = random.randint(0, len(noise_lines))
        noise_lines.insert(pos, base_log)
        log_text = "\n".join(noise_lines)

        rows.append({"log_text": log_text, "failure_type": failure_type})

    df = pd.DataFrame(rows)

    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(output_path, index=False)
    print(f"Generated {n_samples} synthetic samples -> {output_path}")
    print(f"Class distribution:\n{df['failure_type'].value_counts()}\n")
    return df


# ── Model Training ─────────────────────────────────────────────────────────────

def train_model(df: pd.DataFrame, max_features: int = 500):
    """
    Phase 1: Vectorize logs with TF-IDF, train LightGBM classifier.
    Returns (model, vectorizer, X_test_dense, y_test).
    """
    print(f"=== Phase 1: TF-IDF Vectorization (max_features={max_features}) ===")
    vectorizer = TfidfVectorizer(
        max_features=max_features,
        ngram_range=(1, 2),
        stop_words="english",
        sublinear_tf=True,
        min_df=2,
        analyzer="word",
        token_pattern=r"(?u)\b\w\w+\b",
    )

    X = vectorizer.fit_transform(df["log_text"])
    y = df["failure_type"].map(LABEL_MAP).values

    print(f"Feature matrix: {X.shape[0]} samples × {X.shape[1]} features")

    # Stratified split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print(f"\n=== Phase 2: Training LightGBM Classifier ===")
    model = lgb.LGBMClassifier(
        n_estimators=300,
        max_depth=8,
        learning_rate=0.05,
        num_leaves=31,
        objective="multiclass",
        num_class=len(FAILURE_TYPES),
        random_state=42,
        verbose=-1,
        n_jobs=-1,
        subsample=0.8,
        colsample_bytree=0.8,
        reg_alpha=0.1,
        reg_lambda=0.1,
    )

    model.fit(
        X_train.toarray(), y_train,
        eval_set=[(X_test.toarray(), y_test)],
        callbacks=[lgb.early_stopping(50, verbose=False), lgb.log_evaluation(period=0)],
    )

    return model, vectorizer, X_test.toarray(), y_test


# ── Model Evaluation ───────────────────────────────────────────────────────────

def evaluate_model(model, vectorizer, X_test, y_test, X_full, y_full) -> dict:
    """Print full evaluation and return metrics dict."""
    print("\n=== Evaluation: Test Set ===")
    y_pred = model.predict(X_test)
    test_acc = accuracy_score(y_test, y_pred)

    print(classification_report(y_test, y_pred, target_names=FAILURE_TYPES, digits=3))

    print("Confusion Matrix:")
    cm = confusion_matrix(y_test, y_pred)
    print(cm)

    print(f"\n=== Evaluation: Cross-Validation (5-fold, stratified) ===")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_scores = cross_val_score(model, X_full, y_full, cv=cv, scoring="accuracy", n_jobs=-1)
    print(f"CV Accuracy: {cv_scores.mean():.4f} ± {cv_scores.std():.4f}")
    print(f"CV Scores:   {[f'{s:.4f}' for s in cv_scores]}")

    return {
        "test_accuracy": round(float(test_acc), 4),
        "cv_accuracy_mean": round(float(cv_scores.mean()), 4),
        "cv_accuracy_std": round(float(cv_scores.std()), 4),
    }


# ── ONNX Export ────────────────────────────────────────────────────────────────

def export_onnx(model, vectorizer, output_path: str, max_features: int, metrics: dict):
    """
    Export the LightGBM model to ONNX format using onnxmltools.
    Also saves vocabulary JSON and metadata JSON for the Java OnnxFailureClassifier.
    """
    print(f"\n=== ONNX Export ===")

    try:
        import onnxmltools
        from onnxmltools.convert import convert_lightgbm
        from onnxmltools.convert.common.data_types import FloatTensorType
    except ImportError as e:
        print(f"ERROR: onnxmltools not available: {e}")
        print("Install with: pip install onnxmltools")
        sys.exit(1)

    # Export ONNX model
    n_features = len(vectorizer.get_feature_names_out())
    onnx_model = convert_lightgbm(
        model,
        initial_types=[("features", FloatTensorType([None, n_features]))],
        target_opset=12,
    )

    out_path = Path(output_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, "wb") as f:
        f.write(onnx_model.SerializeToString())

    model_size_kb = os.path.getsize(output_path) / 1024
    print(f"ONNX model saved  -> {output_path}  ({model_size_kb:.1f} KB)")

    # ── Vocabulary JSON (used by Java OnnxFailureClassifier) ──────────────────
    vocab_path = str(out_path.with_suffix("")) + ".vocab.json"
    vocab_data = {
        "feature_names": vectorizer.get_feature_names_out().tolist(),
        "max_features": max_features,
        "actual_features": n_features,
        "class_labels": FAILURE_TYPES,
        "label_map": LABEL_MAP,
        "inv_label_map": {str(v): k for k, v in LABEL_MAP.items()},
        "ngram_range": list(vectorizer.ngram_range),
        "sublinear_tf": vectorizer.sublinear_tf,
    }
    with open(vocab_path, "w", encoding="utf-8") as f:
        json.dump(vocab_data, f, indent=2)
    print(f"Vocabulary saved  -> {vocab_path}")

    # ── Metadata JSON (model versioning + metrics for Java ModelVersion entity) ─
    import datetime
    metadata = {
        "model_name": "log-classifier",
        "version": "1.0.0",
        "model_file": str(out_path.name),
        "trained_at": datetime.datetime.utcnow().isoformat() + "Z",
        "framework": "LightGBM",
        "export_format": "ONNX",
        "opset_version": 12,
        "n_features": n_features,
        "max_features_config": max_features,
        "class_labels": FAILURE_TYPES,
        "n_classes": len(FAILURE_TYPES),
        "metrics": metrics,
        "hyperparameters": {
            "n_estimators": model.n_estimators_,
            "max_depth": model.max_depth,
            "learning_rate": model.learning_rate,
            "num_leaves": model.num_leaves,
        },
        "feature_schema": {
            "input_name": "features",
            "input_shape": [None, n_features],
            "input_dtype": "float32",
            "output_name": "probabilities",
            "output_shape": [None, len(FAILURE_TYPES)],
        },
    }
    meta_path = str(out_path.with_suffix("")) + ".metadata.json"
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Metadata saved    -> {meta_path}")

    print(f"\n✅ Training complete!")
    print(f"   Test accuracy : {metrics['test_accuracy']:.4f}")
    print(f"   CV accuracy   : {metrics['cv_accuracy_mean']:.4f} ± {metrics['cv_accuracy_std']:.4f}")
    print(f"\nTo enable ONNX in the Java app, set in application.yml:")
    print(f"  rootcause.classifier.onnx.enabled: true")
    print(f"  rootcause.classifier.onnx.model-path: models/log-classifier.onnx")


# ── CLI ────────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Train CI failure log classifier (Phase 2 — LightGBM → ONNX)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("--data", type=str,
                        help="Path to training CSV (columns: log_text, failure_type)")
    parser.add_argument("--output", type=str, default="../models/log-classifier.onnx",
                        help="Output ONNX model path (default: ../models/log-classifier.onnx)")
    parser.add_argument("--generate-data", action="store_true",
                        help="Generate synthetic training data before training")
    parser.add_argument("--samples", type=int, default=1000,
                        help="Number of synthetic samples to generate (default: 1000)")
    parser.add_argument("--max-features", type=int, default=500,
                        help="Max TF-IDF features (default: 500)")
    parser.add_argument("--skip-onnx", action="store_true",
                        help="Skip ONNX export (train + evaluate only)")
    args = parser.parse_args()

    print("=" * 60)
    print(" AI-Assisted CI-Failure Root-Cause Suggester — ML Pipeline")
    print("=" * 60 + "\n")

    # ── Step 1: Load or generate data ──────────────────────────────────────────
    if args.generate_data:
        data_path = args.data or "training_data.csv"
        df = generate_sample_data(data_path, args.samples)
    elif args.data:
        df = load_data(args.data)
    else:
        print("ERROR: Provide --data <csv_file> or --generate-data")
        parser.print_help()
        sys.exit(1)

    if len(df) < 50:
        print(f"ERROR: Too few samples ({len(df)}). Need at least 50.")
        sys.exit(1)

    # ── Step 2: Train ──────────────────────────────────────────────────────────
    model, vectorizer, X_test, y_test = train_model(df, args.max_features)

    # ── Step 3: Full feature matrix for CV ────────────────────────────────────
    X_full = vectorizer.transform(df["log_text"]).toarray()
    y_full = df["failure_type"].map(LABEL_MAP).values

    # ── Step 4: Evaluate ───────────────────────────────────────────────────────
    metrics = evaluate_model(model, vectorizer, X_test, y_test, X_full, y_full)

    # ── Step 5: Export ─────────────────────────────────────────────────────────
    if not args.skip_onnx:
        export_onnx(model, vectorizer, args.output, args.max_features, metrics)
    else:
        print("\n[Skipped ONNX export (--skip-onnx)]")


if __name__ == "__main__":
    main()

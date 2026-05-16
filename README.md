# AI-Assisted CI-Failure Root-Cause Suggester

> **Automatically diagnose CI pipeline failures and suggest actionable fixes.**

A production-grade Java service that analyzes CI job failures from **GitLab CI**, **Jenkins**, or raw log uploads, classifies the root cause (infra / test / build / security), and provides actionable remediation suggestions — with optional ML-powered classification.

---

## ✨ Features

| Feature | Phase | Status |
|---------|-------|--------|
| Rule-based regex pattern matching | Phase 1 | ✅ |
| REST API (`/api/v1/analyze/raw`) | Phase 1 | ✅ |
| 22+ pre-built Java CI failure patterns | Phase 1 | ✅ |
| PostgreSQL persistence (Flyway migrations) | Phase 1 | ✅ |
| ML classification (LightGBM → ONNX → Java) | Phase 2 | ✅ |
| Hybrid mode (rule + ML, pick best) | Phase 2 | ✅ |
| GitLab CI webhook integration | Phase 3 | ✅ |
| GitLab MR comment posting | Phase 3 | ✅ |
| Jenkins shared library | Phase 3 | ✅ |
| Prometheus metrics + Grafana dashboard | Phase 3 | ✅ |
| CLI tool | Phase 4 | ✅ |
| Markdown report generation | Phase 4 | ✅ |
| Swagger/OpenAPI docs | All | ✅ |

---

## 🏗️ Architecture

```
GitLab CI / Jenkins / CLI
        │
        ▼
┌─────────────────────────────┐
│   FailureRootCauseController │  ← REST API
│   GitLabWebhookController    │  ← Webhook receiver
└────────────┬────────────────┘
             │
     ┌───────▼────────┐
     │ Classification  │
     │   Service       │
     ├────────┬────────┤
     │ Rule   │  ONNX  │  ← Pluggable classifiers
     │ Based  │   ML   │
     └────────┴────┬───┘
                   │
     ┌─────────────▼──────┐
     │ PostgreSQL + Flyway │  ← Patterns, jobs, analyses
     └────────────────────┘
                   │
     ┌─────────────▼──────┐
     │ Prometheus + Grafana│  ← Observability
     └────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- (Optional) Python 3.11+ for ML training

### Run with Docker Compose

```bash
# Start all services (API + PostgreSQL + Prometheus + Grafana)
docker compose up -d

# The API is available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
# Grafana at http://localhost:3000 (admin/admin)
# Prometheus at http://localhost:9090
```

### Run locally (development)

```bash
# Start PostgreSQL only
docker compose up -d postgres

# Run the application
./gradlew bootRun

# Run tests
./gradlew test
```

---

## 📡 API Usage

### Analyze raw log content

```bash
curl -X POST http://localhost:8080/api/v1/analyze/raw \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "my-org/my-app",
    "branchName": "feature/auth",
    "commitSha": "abc123",
    "ciPlatform": "GITLAB",
    "logContent": "java.net.ConnectException: Connection refused\nFailed to connect to database"
  }'
```

**Response:**

```json
{
  "analysisId": "...",
  "jobId": "...",
  "failureType": "infra",
  "confidence": 0.90,
  "explanation": "Network connection refused: Connection refused. Likely a transient infrastructure issue.",
  "suggestedAction": "Verify the target service is running and reachable. Check firewall rules and retry.",
  "classifierMode": "rule_based",
  "matchedPatternName": "Connection Refused"
}
```

### Analyze a stored job

```bash
curl -X POST http://localhost:8080/api/v1/jobs/{jobId}/analyze \
  -H "Content-Type: application/json" \
  -d '{"mode": "RULE_BASED"}'
```

### Get Markdown report

```bash
curl http://localhost:8080/api/v1/jobs/{jobId}/report
```

---

## 🔗 GitLab CI Integration

Add the `analyze-failure` stage to your `.gitlab-ci.yml`:

```yaml
analyze-failure:
  stage: analyze-failure
  when: on_failure
  image: alpine:3.19
  before_script:
    - apk add --no-cache curl jq
  script:
    - |
      curl -s -X POST "${ROOTCAUSE_API_URL}/api/v1/analyze/raw" \
        -H "Content-Type: application/json" \
        -d "{\"projectName\":\"${CI_PROJECT_PATH}\",\"ciPlatform\":\"GITLAB\",\"logContent\":\"...\"}"
```

Or set up a **GitLab Webhook** for automatic analysis:

1. Go to **Settings → Webhooks**
2. URL: `https://your-api/webhooks/gitlab/pipeline`
3. Secret token: (match `rootcause.gitlab.webhook-secret`)
4. Trigger: **Pipeline events**

See [.gitlab-ci.yml](.gitlab-ci.yml) for a complete example.

---

## 🧪 ML Training (Phase 2)

```bash
cd ml
pip install -r requirements.txt

# Generate synthetic training data and train model
python train.py --generate-data --samples 1000 --output ../models/log-classifier.onnx

# Enable ONNX in config
# rootcause.classifier.onnx.enabled=true
```

---

## 💻 CLI Tool

```bash
# Analyze a log file
java -cp rootcause-suggester.jar com.rootcause.cli.RootCauseCli \
  --api-url http://localhost:8080 \
  --log-file build/test-output.log \
  --output-format markdown

# Analyze inline text
java -cp rootcause-suggester.jar com.rootcause.cli.RootCauseCli \
  --api-url http://localhost:8080 \
  --log-text "OutOfMemoryError: Java heap space" \
  --output-format json
```

---

## 📊 Observability

| Metric | Type | Description |
|--------|------|-------------|
| `rootcause_job_failure_total` | Counter | Total failure events |
| `rootcause_failure_type_count` | Counter | By failure category |
| `rootcause_model_confidence` | Histogram | Confidence distribution |
| `rootcause_analyze_duration_seconds` | Timer | Analysis latency |
| `rootcause_pattern_match_count` | Counter | Per pattern hits |

Grafana dashboard auto-provisions at http://localhost:3000 with Docker Compose.

---

## 📁 Project Structure

```
├── src/main/java/com/rootcause/
│   ├── domain/          # JPA entities + enums
│   ├── repository/      # Spring Data JPA
│   ├── service/         # Business logic
│   ├── classifier/      # Rule-based + ONNX classifiers
│   ├── web/             # REST controllers + DTOs
│   ├── integration/     # GitLab + Jenkins adapters
│   ├── config/          # Spring configuration
│   ├── metrics/         # Prometheus metrics
│   └── cli/             # CLI tool
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway SQL
├── ml/                  # Python training pipeline
├── docker/              # Dockerfile, Prometheus, Grafana
├── docs/                # Jenkins shared lib, runbooks
├── .gitlab-ci.yml       # GitLab CI integration example
└── docker-compose.yml
```

---

## 🛡️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ML Runtime | ONNX Runtime (Java) |
| ML Training | Python + LightGBM + scikit-learn |
| Metrics | Micrometer + Prometheus |
| Dashboard | Grafana |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Container | Docker + Docker Compose |
| CI | GitLab CI/CD |

---

## 📄 License

MIT

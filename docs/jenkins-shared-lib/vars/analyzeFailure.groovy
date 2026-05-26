// =============================================================================
// Jenkins Shared Library: analyzeFailure.groovy
// Production-grade step for AI-Assisted CI-Failure Root-Cause Suggester
// =============================================================================
//
// Installation
// ─────────────
// 1. Host this repo as a Jenkins Shared Library:
//    Manage Jenkins → Configure System → Global Pipeline Libraries
//    Name: rootcause-lib, Default version: main
//
// 2. Reference in your Jenkinsfile:
//
//   @Library('rootcause-lib') _
//
//   pipeline {
//       agent any
//       environment {
//           ROOTCAUSE_API_URL   = credentials('rootcause-api-url')
//           ROOTCAUSE_API_KEY   = credentials('rootcause-api-key')   // optional
//       }
//       stages {
//           stage('Build & Test') {
//               steps { sh './gradlew test' }
//           }
//       }
//       post {
//           failure {
//               analyzeFailure(
//                   logFile: 'build/reports/tests/test/TEST-*.xml',  // optional
//                   mode:    'RULE_BASED'                             // optional
//               )
//           }
//       }
//   }
// =============================================================================

/**
 * Analyze the current Jenkins build failure using the RootCause API.
 *
 * @param config Map with optional keys:
 *   - apiUrl      : API base URL (fallback: $ROOTCAUSE_API_URL env var)
 *   - apiKey      : X-API-Key value (fallback: $ROOTCAUSE_API_KEY env var)
 *   - logFile     : glob pattern to extra log files to append (optional)
 *   - mode        : RULE_BASED | ML_ONNX | HYBRID (default: RULE_BASED)
 *   - maxLogChars : log size cap in chars (default: 900_000 ≈ 900 KB)
 *   - failOnError : if true, throw on API failure (default: false)
 */
def call(Map config = [:]) {
    def apiUrl      = config.apiUrl      ?: env.ROOTCAUSE_API_URL ?: 'http://localhost:8080'
    def apiKey      = config.apiKey      ?: env.ROOTCAUSE_API_KEY ?: ''
    def mode        = config.mode        ?: 'RULE_BASED'
    def maxLogChars = config.maxLogChars ?: 900_000
    def failOnError = config.failOnError ?: false

    echo "🔍 RootCause: Starting CI failure analysis (mode=${mode})..."

    // ── 1. Collect log ──────────────────────────────────────────────────────
    def logText = collectLog(config.logFile, maxLogChars)

    // ── 2. Build request ────────────────────────────────────────────────────
    def payload = groovy.json.JsonOutput.toJson([
        projectName: env.JOB_NAME     ?: 'unknown',
        pipelineRef: env.BUILD_NUMBER ?: 'unknown',
        branchName:  env.BRANCH_NAME  ?: env.GIT_BRANCH ?: 'unknown',
        commitSha:   env.GIT_COMMIT   ?: 'unknown',
        ciPlatform:  'JENKINS',
        logContent:  logText,
        mode:        mode
    ])

    // ── 3. Call API ─────────────────────────────────────────────────────────
    def result = callApi(apiUrl, apiKey, payload, failOnError)
    if (!result) return null

    // ── 4. Display result ───────────────────────────────────────────────────
    printResult(result)

    // ── 5. Update build description ─────────────────────────────────────────
    def pct = String.format('%.0f%%', (result.confidence ?: 0) * 100)
    currentBuild.description = "🔍 ${result.failureType?.toUpperCase()} (${pct}) — ${truncate(result.suggestedAction ?: '', 80)}"

    // ── 6. Write machine-readable artifact ──────────────────────────────────
    writeJSON file: 'rootcause-analysis.json', json: result, pretty: 2
    archiveArtifacts artifacts: 'rootcause-analysis.json', allowEmptyArchive: true

    echo "✅ RootCause analysis complete. Artifact: rootcause-analysis.json"
    return result
}

// ── Private helpers ──────────────────────────────────────────────────────────

private String collectLog(String logFileGlob, int maxLogChars) {
    def sb = new StringBuilder()

    // Console log from Jenkins
    try {
        def lines = currentBuild.rawBuild.getLog(10_000)
        sb.append(lines.join('\n'))
    } catch (Exception e) {
        echo "⚠️  Could not read console log: ${e.message}"
        sb.append("Jenkins build ${env.BUILD_URL} failed.\n")
    }

    // Optional extra log file (test reports, surefire XML, etc.)
    if (logFileGlob) {
        try {
            def files = findFiles(glob: logFileGlob)
            for (f in files) {
                sb.append('\n--- ').append(f.name).append(' ---\n')
                sb.append(readFile(file: f.path))
            }
        } catch (Exception e) {
            echo "⚠️  Could not read log file '${logFileGlob}': ${e.message}"
        }
    }

    def log = sb.toString()

    // Cap size — keep the tail (most recent / most useful part)
    if (log.length() > maxLogChars) {
        echo "⚠️  Log truncated from ${log.length()} to ${maxLogChars} chars (tail kept)"
        log = log.substring(log.length() - maxLogChars)
    }

    return log
}

private Object callApi(String apiUrl, String apiKey, String payload, boolean failOnError) {
    def headers = [[name: 'Content-Type', value: 'application/json']]
    if (apiKey) {
        headers << [name: 'X-API-Key', value: apiKey]
    }

    try {
        def response = httpRequest(
            url:                "${apiUrl}/api/v1/analyze/raw",
            httpMode:           'POST',
            customHeaders:      headers,
            requestBody:        payload,
            validResponseCodes: '200',
            quiet:              true,
            timeout:            60
        )
        return readJSON(text: response.content)
    } catch (Exception e) {
        echo "⚠️  RootCause API call failed: ${e.message}"
        if (failOnError) throw e
        return null
    }
}

private void printResult(def r) {
    def confidence = String.format('%.0f%%', (r.confidence ?: 0) * 100)
    echo """\

╔══════════════════════════════════════════════════════════════╗
║              CI FAILURE ROOT-CAUSE ANALYSIS                  ║
╠══════════════════════════════════════════════════════════════╣
║  Failure Type : ${pad(r.failureType?.toUpperCase(), 43)}║
║  Confidence   : ${pad(confidence, 43)}║
║  Classifier   : ${pad(r.classifierMode, 43)}║
║  Pattern      : ${pad(r.matchedPatternName ?: 'N/A', 43)}║
╠══════════════════════════════════════════════════════════════╣
║  Explanation:                                                ║
║  ${pad(truncate(r.explanation ?: 'N/A', 59), 59)}║
╠══════════════════════════════════════════════════════════════╣
║  Suggested Action:                                           ║
║  ${pad(truncate(r.suggestedAction ?: 'N/A', 59), 59)}║
╚══════════════════════════════════════════════════════════════╝
"""
}

private String pad(String s, int len) {
    s = s ?: 'N/A'
    if (s.length() >= len) return s.substring(0, len)
    return s + ' ' * (len - s.length())
}

private String truncate(String s, int maxLen) {
    if (!s) return 'N/A'
    return s.length() > maxLen ? s.substring(0, maxLen - 3) + '...' : s
}


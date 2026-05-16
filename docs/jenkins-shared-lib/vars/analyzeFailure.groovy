// =============================================================================
// Jenkins Shared Library: analyzeFailure.groovy
// =============================================================================
// Usage in a Jenkinsfile:
//
//   @Library('rootcause-lib') _
//
//   pipeline {
//       agent any
//       stages {
//           stage('Test') {
//               steps { sh './gradlew test' }
//           }
//       }
//       post {
//           failure {
//               analyzeFailure(
//                   apiUrl: env.ROOTCAUSE_API_URL,
//                   token:  env.ROOTCAUSE_API_TOKEN
//               )
//           }
//       }
//   }
// =============================================================================

def call(Map config = [:]) {
    def apiUrl = config.apiUrl ?: env.ROOTCAUSE_API_URL ?: 'http://localhost:8080'
    def token  = config.token  ?: env.ROOTCAUSE_API_TOKEN ?: ''
    def maxLogLines = config.maxLogLines ?: 5000

    echo "🔍 Running CI Failure Root-Cause Analysis..."

    // Collect console log
    def logText = ''
    try {
        logText = currentBuild.rawBuild.getLog(maxLogLines).join('\n')
    } catch (Exception e) {
        echo "⚠️  Could not retrieve build log: ${e.message}"
        logText = "Jenkins build failed. Pipeline: ${env.BUILD_URL}"
    }

    // Truncate if too large
    if (logText.length() > 100000) {
        logText = logText.substring(logText.length() - 100000)
    }

    // Build request payload
    def payload = groovy.json.JsonOutput.toJson([
        projectName: env.JOB_NAME ?: 'unknown',
        pipelineRef: env.BUILD_NUMBER ?: 'unknown',
        branchName:  env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown',
        commitSha:   env.GIT_COMMIT ?: 'unknown',
        ciPlatform:  'JENKINS',
        logContent:  logText
    ])

    // Call RootCause API
    try {
        def response = httpRequest(
            url:            "${apiUrl}/api/v1/analyze/raw",
            httpMode:       'POST',
            contentType:    'APPLICATION_JSON',
            customHeaders:  [[name: 'Authorization', value: "Bearer ${token}"]],
            requestBody:    payload,
            validResponseCodes: '200',
            quiet:          true
        )

        def result = readJSON(text: response.content)

        // Print results
        echo """
╔══════════════════════════════════════════════════════════════╗
║            CI FAILURE ROOT-CAUSE ANALYSIS                    ║
╠══════════════════════════════════════════════════════════════╣
║ Failure Type : ${result.failureType?.padRight(42)}║
║ Confidence   : ${String.format('%.0f%%', (result.confidence ?: 0) * 100).padRight(42)}║
║ Classifier   : ${(result.classifierMode ?: 'N/A').padRight(42)}║
╠══════════════════════════════════════════════════════════════╣
║ Explanation:                                                 ║
║ ${truncate(result.explanation ?: 'N/A', 58).padRight(60)}║
╠══════════════════════════════════════════════════════════════╣
║ Suggested Action:                                            ║
║ ${truncate(result.suggestedAction ?: 'N/A', 58).padRight(60)}║
╚══════════════════════════════════════════════════════════════╝
"""

        // Add to build description
        currentBuild.description = "🔍 ${result.failureType} (${String.format('%.0f%%', (result.confidence ?: 0) * 100)})"

        return result

    } catch (Exception e) {
        echo "⚠️  RootCause analysis failed: ${e.message}"
        echo "The pipeline failure analysis could not be completed."
        return null
    }
}

private String truncate(String text, int maxLen) {
    if (text == null) return 'N/A'
    return text.length() > maxLen ? text.substring(0, maxLen - 3) + '...' : text
}

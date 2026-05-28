// =============================================================================
// Jenkinsfile — Pipeline for AI-Assisted CI-Failure Root-Cause Suggester
// =============================================================================

@Library('rootcause-lib') _

pipeline {
    agent any

    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
        ansiColor('xterm')
    }

    environment {
        // Suggester API parameters (setup credentials in Jenkins if needed)
        // ROOTCAUSE_API_URL: Jenkins will fetch this from credentials or fallback to default
        ROOTCAUSE_API_URL = credentials('rootcause-api-url')
        ROOTCAUSE_API_KEY = credentials('rootcause-api-key') // optional API key
    }

    stages {
        stage('Initialize') {
            steps {
                echo '=== Initializing Build ==='
                sh 'java -version'
                sh 'chmod +x gradlew'
                sh './gradlew --version'
            }
        }

        stage('Compile') {
            steps {
                echo '=== Compiling ==='
                sh './gradlew compileJava compileTestJava --no-daemon'
            }
        }

        stage('Test & Build') {
            steps {
                echo '=== Running Suite ==='
                sh './gradlew build --no-daemon'
            }
        }
    }

    post {
        always {
            // Publish test reports in Jenkins
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
        }

        failure {
            script {
                echo '❌ Pipeline failed! Initiating AI-assisted failure classification...'
                try {
                    // Method A: Shared Library Step (recommended)
                    // Leverages the custom analyzeFailure step to perform log extraction, NLP classification,
                    // custom console UI rendering, updating build descriptions, and archiving JSON results.
                    analyzeFailure(
                        mode: 'HYBRID',
                        logFile: '**/build/test-results/test/*.xml'
                    )
                } catch (Exception e) {
                    echo "⚠️ Shared Library analyzeFailure step unavailable: ${e.getMessage()}"
                    echo "🔗 Fallback: Posting event notification directly to the Jenkins webhook endpoint..."

                    // Method B: Webhook Fallback
                    // Posts job info back to our JenkinsWebhookController. The controller will asynchronously
                    // fetch the console log from Jenkins and update the build description.
                    try {
                        withCredentials([string(credentialsId: 'rootcause-jenkins-token', variable: 'JENKINS_TOKEN')]) {
                            sh """
                                curl -s -X POST \
                                  -H "Content-Type: application/json" \
                                  -H "X-Jenkins-Token: \${JENKINS_TOKEN}" \
                                  -d '{"name": "\${JOB_NAME}", "url": "\${JOB_URL}", "build": {"number": \${BUILD_NUMBER}, "phase": "COMPLETED", "status": "FAILURE", "full_url": "\${BUILD_URL}"}}' \
                                  \${ROOTCAUSE_API_URL}/webhooks/jenkins/build || echo "Webhook endpoint unreachable"
                            """
                        }
                    } catch (Exception ex) {
                        echo "⚠️ Webhook fallback failed: ${ex.getMessage()}"
                    }
                }
            }
        }
    }
}

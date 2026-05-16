-- V2: Seed common Java CI failure patterns
-- ==========================================

-- ─── INFRA patterns ───────────────────────────────────────────────

INSERT INTO failure_pattern (id, name, failure_type, regex_pattern, explanation_template, suggested_action_template, priority, active)
VALUES
(gen_random_uuid(), 'Connection Refused', 'INFRA',
 '(?i)(connection\s+refused|connect\s+timed?\s*out|ECONNREFUSED)',
 'Network connection refused: {matched_text}. Likely a transient infrastructure issue or the target service is down.',
 'Verify the target service is running and reachable. Check firewall rules and retry the pipeline.', 90, true),

(gen_random_uuid(), 'OOM Killed', 'INFRA',
 '(?i)(out\s*of\s*memory|OOM\s*Kill|java\.lang\.OutOfMemoryError|Cannot allocate memory|oom-kill)',
 'Process killed due to out-of-memory: {matched_text}.',
 'Increase memory limits for the CI runner/container. Consider optimizing memory usage in tests.', 95, true),

(gen_random_uuid(), 'Disk Space Exhausted', 'INFRA',
 '(?i)(no\s+space\s+left\s+on\s+device|disk\s+quota\s+exceeded|ENOSPC)',
 'Disk space exhausted on CI runner: {matched_text}.',
 'Clean up CI caches and artifacts. Increase disk allocation for the runner.', 85, true),

(gen_random_uuid(), 'DNS Resolution Failure', 'INFRA',
 '(?i)(unknown\s+host|name\s+or\s+service\s+not\s+known|DNS\s+resolution\s+failed|NXDOMAIN|could\s+not\s+resolve\s+host)',
 'DNS resolution failed: {matched_text}.',
 'Check DNS configuration and network connectivity. Verify the hostname is correct.', 80, true),

(gen_random_uuid(), 'Docker Daemon Unavailable', 'INFRA',
 '(?i)(cannot\s+connect\s+to\s+the\s+docker\s+daemon|docker\.sock|Is the docker daemon running)',
 'Docker daemon is unavailable: {matched_text}.',
 'Ensure Docker service is running on the CI runner. Check Docker-in-Docker or socket permissions.', 88, true),

(gen_random_uuid(), 'Database Connection Pool Exhaustion', 'INFRA',
 '(?i)(connection\s+pool\s+exhausted|HikariPool.*Connection is not available|Unable to acquire JDBC Connection|too many connections)',
 'Database connection pool exhausted: {matched_text}.',
 'Check DB connection pool settings (HikariCP). Look for connection leaks in test teardown.', 87, true),

(gen_random_uuid(), 'SSL/TLS Handshake Failure', 'INFRA',
 '(?i)(SSL\s*handshake|PKIX\s+path\s+building\s+failed|certificate\s+verify\s+failed|SSLHandshakeException)',
 'SSL/TLS handshake failure: {matched_text}.',
 'Update CA certificates on the CI runner. Check if the target certificate is valid and not expired.', 75, true),

(gen_random_uuid(), 'CI Runner Timeout', 'INFRA',
 '(?i)(job\s+execution\s+timeout|exceeded\s+the\s+maximum\s+allowed\s+timeout|runner\s+timed?\s*out|stuck\s+or\s+timeout)',
 'CI job timed out: {matched_text}.',
 'Increase job timeout. Investigate long-running tests or deadlocks.', 82, true),

-- ─── TEST patterns ────────────────────────────────────────────────

(gen_random_uuid(), 'JUnit Test Failure', 'TEST',
 '(?i)(Tests?\s+run:.*Failures?:\s*[1-9]|FAILED\s+test|test.*FAILED|AssertionError|AssertionFailedError)',
 'Unit/integration test failures detected: {matched_text}.',
 'Review the failing test cases. Check recent code changes that may have broken assertions.', 70, true),

(gen_random_uuid(), 'NullPointerException in Test', 'TEST',
 '(?i)(java\.lang\.NullPointerException)',
 'NullPointerException encountered: {matched_text}.',
 'Review the stack trace to identify null references. Add null-safety checks or fix test data setup.', 72, true),

(gen_random_uuid(), 'Test Compilation Error', 'TEST',
 '(?i)(error:.*cannot find symbol|error:.*method.*not found|Compilation failure.*test)',
 'Test compilation error: {matched_text}.',
 'Fix compilation errors in test source code. Verify API changes are reflected in tests.', 74, true),

(gen_random_uuid(), 'Flaky Test - Timeout', 'TEST',
 '(?i)(test.*timed?\s*out|SocketTimeoutException.*test|TestTimedOutException|Awaitility.*ConditionTimeoutException)',
 'Test timed out (possible flaky test): {matched_text}.',
 'Increase test timeout or investigate why the test is slow. Consider marking as @Flaky and filing a bug.', 68, true),

(gen_random_uuid(), 'Spring Context Failure', 'TEST',
 '(?i)(Failed to load ApplicationContext|Error creating bean|BeanCreationException|NoSuchBeanDefinitionException)',
 'Spring application context failed to load: {matched_text}.',
 'Check Spring configuration and bean definitions. Verify test profile settings and @MockBean usage.', 76, true),

-- ─── BUILD patterns ───────────────────────────────────────────────

(gen_random_uuid(), 'Gradle Build Failure', 'BUILD',
 '(?i)(BUILD FAILED|Execution failed for task|Could not resolve.*dependencies|Gradle build daemon disappeared)',
 'Gradle build failed: {matched_text}.',
 'Check build.gradle for syntax errors or unresolvable dependencies. Clear Gradle caches and retry.', 80, true),

(gen_random_uuid(), 'Maven Build Failure', 'BUILD',
 '(?i)(BUILD FAILURE|\[ERROR\].*Failed to execute goal|Could not resolve dependencies|Non-resolvable parent POM)',
 'Maven build failed: {matched_text}.',
 'Check pom.xml for dependency issues. Verify Maven repository availability and settings.xml.', 80, true),

(gen_random_uuid(), 'Java Compilation Error', 'BUILD',
 '(?i)(error:.*java:\d+:.*error|COMPILATION ERROR|javac.*error|compiler\.err)',
 'Java compilation failed: {matched_text}.',
 'Fix the compilation errors in source code. Check for incompatible Java version or missing imports.', 85, true),

(gen_random_uuid(), 'Dependency Resolution Failure', 'BUILD',
 '(?i)(Could not find artifact|Could not resolve|Failed to collect dependencies|artifact.*not found|repository.*unreachable)',
 'Dependency resolution failed: {matched_text}.',
 'Check if the artifact repository is available. Verify dependency coordinates and version numbers.', 78, true),

(gen_random_uuid(), 'Checkstyle / Lint Violation', 'BUILD',
 '(?i)(Checkstyle\s+rule|checkstyle.*violation|SpotBugs.*found|PMD.*violation|lint.*error)',
 'Code quality check failed: {matched_text}.',
 'Fix the reported code style or quality violations. Run the check locally before pushing.', 60, true),

-- ─── SECURITY patterns ───────────────────────────────────────────

(gen_random_uuid(), 'Dependency Vulnerability (CVE)', 'SECURITY',
 '(?i)(CVE-\d{4}-\d+|vulnerability\s+found|security\s+scan\s+failed|OWASP.*fail|dependencyCheck.*fail)',
 'Security vulnerability detected: {matched_text}.',
 'Update the vulnerable dependency to a patched version. Check CVE details for severity and workarounds.', 90, true),

(gen_random_uuid(), 'Secret Leak Detection', 'SECURITY',
 '(?i)(secret\s+detected|credential\s+leak|API\s+key\s+exposed|password\s+in\s+source|gitleaks.*found)',
 'Potential secret/credential leak detected: {matched_text}.',
 'Remove the leaked secret from source code immediately. Rotate the exposed credentials. Use CI secret variables.', 99, true),

(gen_random_uuid(), 'Container Image Vulnerability', 'SECURITY',
 '(?i)(trivy.*CRITICAL|grype.*vulnerability|container\s+scan.*fail|image\s+scan.*found)',
 'Container image vulnerability scan failed: {matched_text}.',
 'Update the base image or vulnerable packages. Review the scan report and apply patches.', 88, true);

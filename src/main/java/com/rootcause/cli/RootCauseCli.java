package com.rootcause.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
//import java.util.Arrays;

/**
 * CLI tool for analyzing CI failures from the command line.
 *
 * Usage:
 * java -cp rootcause-suggester.jar com.rootcause.cli.RootCauseCli \
 * --api-url http://localhost:8080 \
 * --log-file build/test-output.log \
 * --output-format markdown
 *
 * Or with a job ID:
 * java -cp rootcause-suggester.jar com.rootcause.cli.RootCauseCli \
 * --api-url http://localhost:8080 \
 * --job-id abc-123 \
 * --output-format json
 */
public class RootCauseCli {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        CliConfig config = parseArgs(args);

        if (config.showHelp) {
            printHelp();
            return;
        }

        if (config.apiUrl == null) {
            System.err.println("Error: --api-url is required");
            printHelp();
            System.exit(1);
        }

        JsonNode result;

        if (config.jobId != null) {
            result = analyzeByJobId(config);
        } else if (config.logFile != null) {
            result = analyzeLogFile(config);
        } else if (config.logText != null) {
            result = analyzeLogText(config);
        } else {
            System.err.println("Error: provide --job-id, --log-file, or --log-text");
            printHelp();
            System.exit(1);
            return;
        }

        outputResult(result, config);
    }

    private static JsonNode analyzeByJobId(CliConfig config) throws Exception {
        String url = config.apiUrl + "/api/v1/jobs/" + config.jobId + "/analyze";
        String body = "{}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (config.token != null ? config.token : ""))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return MAPPER.readTree(response.body());
    }

    private static JsonNode analyzeLogFile(CliConfig config) throws Exception {
        String logContent = Files.readString(Path.of(config.logFile));
        config.logText = logContent;
        return analyzeLogText(config);
    }

    private static JsonNode analyzeLogText(CliConfig config) throws Exception {
        String url = config.apiUrl + "/api/v1/analyze/raw";

        String payload = MAPPER.writeValueAsString(new java.util.LinkedHashMap<>() {
            {
                put("projectName", config.projectName != null ? config.projectName : "cli-analysis");
                put("ciPlatform", "OTHER");
                put("logContent", config.logText);
                put("mode", config.mode != null ? config.mode : "RULE_BASED");
            }
        });

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (config.token != null ? config.token : ""))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return MAPPER.readTree(response.body());
    }

    private static void checkResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            System.err.println("API Error (HTTP " + response.statusCode() + "): " + response.body());
            System.exit(1);
        }
    }

    private static void outputResult(JsonNode result, CliConfig config) throws IOException {
        String output;

        if ("markdown".equalsIgnoreCase(config.outputFormat)) {
            output = formatMarkdown(result);
        } else if ("json".equalsIgnoreCase(config.outputFormat)) {
            output = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } else {
            output = formatTable(result);
        }

        if (config.outputFile != null) {
            Files.writeString(Path.of(config.outputFile), output);
            System.out.println("Output written to: " + config.outputFile);
        } else {
            System.out.println(output);
        }
    }

    private static String formatMarkdown(JsonNode r) {
        return String.format("""
                # CI Failure Root-Cause Analysis

                | Field | Value |
                |-------|-------|
                | **Failure Type** | `%s` |
                | **Confidence** | %.0f%% |
                | **Classifier** | %s |
                | **Pattern** | %s |

                ## Explanation

                %s

                ## Suggested Action

                %s
                """,
                text(r, "failureType"),
                r.has("confidence") ? r.get("confidence").asDouble() * 100 : 0,
                text(r, "classifierMode"),
                text(r, "matchedPatternName"),
                text(r, "explanation"),
                text(r, "suggestedAction"));
    }

    private static String formatTable(JsonNode r) {
        return String.format("""
                ╔══════════════════════════════════════════════════════════════╗
                ║              CI FAILURE ROOT-CAUSE ANALYSIS                 ║
                ╠══════════════════════════════════════════════════════════════╣
                ║ Failure Type : %-44s║
                ║ Confidence   : %-44s║
                ║ Classifier   : %-44s║
                ║ Pattern      : %-44s║
                ╠══════════════════════════════════════════════════════════════╣
                ║ Explanation:                                                ║
                ║ %-60s║
                ╠══════════════════════════════════════════════════════════════╣
                ║ Suggested Action:                                           ║
                ║ %-60s║
                ╚══════════════════════════════════════════════════════════════╝
                """,
                text(r, "failureType"),
                r.has("confidence") ? String.format("%.0f%%", r.get("confidence").asDouble() * 100) : "N/A",
                text(r, "classifierMode"),
                text(r, "matchedPatternName"),
                truncate(text(r, "explanation"), 60),
                truncate(text(r, "suggestedAction"), 60));
    }

    private static String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "N/A";
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    // ── Arg parsing ────────────────────────────────────────────────

    private static CliConfig parseArgs(String[] args) {
        CliConfig config = new CliConfig();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h" -> config.showHelp = true;
                case "--api-url" -> config.apiUrl = args[++i];
                case "--token" -> config.token = args[++i];
                case "--job-id" -> config.jobId = args[++i];
                case "--log-file" -> config.logFile = args[++i];
                case "--log-text" -> config.logText = args[++i];
                case "--project" -> config.projectName = args[++i];
                case "--mode" -> config.mode = args[++i];
                case "--output-format", "-f" -> config.outputFormat = args[++i];
                case "--output-file", "-o" -> config.outputFile = args[++i];
                default -> System.err.println("Unknown option: " + args[i]);
            }
        }
        return config;
    }

    private static void printHelp() {
        System.out.println("""
                RootCause CLI — CI Failure Root-Cause Analyzer

                Usage:
                  java -cp rootcause-suggester.jar com.rootcause.cli.RootCauseCli [options]

                Options:
                  --api-url URL         RootCause API base URL (required)
                  --token TOKEN         API authentication token
                  --job-id ID           Analyze a stored job by UUID
                  --log-file PATH       Analyze a local log file
                  --log-text TEXT        Analyze inline log text
                  --project NAME        Project name (for raw analysis)
                  --mode MODE           Classifier mode: RULE_BASED, ML_ONNX, HYBRID
                  --output-format FMT   Output format: table (default), json, markdown
                  --output-file PATH    Write output to file instead of stdout
                  --help, -h            Show this help message

                Examples:
                  # Analyze a log file
                  java -cp app.jar com.rootcause.cli.RootCauseCli \\
                    --api-url http://localhost:8080 \\
                    --log-file build/test-output.log \\
                    --output-format markdown

                  # Analyze a stored job
                  java -cp app.jar com.rootcause.cli.RootCauseCli \\
                    --api-url http://localhost:8080 \\
                    --job-id abc-123-def \\
                    --output-format json
                """);
    }

    private static class CliConfig {
        boolean showHelp;
        String apiUrl;
        String token;
        String jobId;
        String logFile;
        String logText;
        String projectName;
        String mode;
        String outputFormat = "table";
        String outputFile;
    }
}

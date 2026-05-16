package com.rootcause;

import com.rootcause.integration.gitlab.GitLabWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitLab Webhook Payload Deserialization Tests")
class GitLabWebhookPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should deserialize pipeline failure webhook")
    void shouldDeserializePipelineWebhook() throws Exception {
        String json = """
                {
                    "object_kind": "pipeline",
                    "object_attributes": {
                        "id": 12345,
                        "ref": "main",
                        "status": "failed",
                        "source": "push",
                        "created_at": "2024-01-15 10:00:00 UTC",
                        "finished_at": "2024-01-15 10:05:00 UTC"
                    },
                    "project": {
                        "id": 100,
                        "name": "my-app",
                        "path_with_namespace": "my-org/my-app",
                        "web_url": "https://gitlab.com/my-org/my-app"
                    },
                    "commit": {
                        "id": "abc123def456",
                        "message": "Fix bug in user service",
                        "author_name": "John Doe"
                    },
                    "merge_request": {
                        "id": 999,
                        "iid": 42,
                        "title": "MR: Fix user service",
                        "state": "opened"
                    },
                    "builds": [
                        {
                            "id": 5001,
                            "name": "test",
                            "stage": "test",
                            "status": "failed",
                            "allow_failure": false
                        },
                        {
                            "id": 5002,
                            "name": "lint",
                            "stage": "test",
                            "status": "success",
                            "allow_failure": false
                        },
                        {
                            "id": 5003,
                            "name": "optional-check",
                            "stage": "test",
                            "status": "failed",
                            "allow_failure": true
                        }
                    ]
                }
                """;

        GitLabWebhookPayload payload = mapper.readValue(json, GitLabWebhookPayload.class);

        assertEquals("pipeline", payload.getObjectKind());
        assertTrue(payload.isPipelineFailure());
        assertEquals("my-org/my-app", payload.getProject().getPathWithNamespace());
        assertEquals("abc123def456", payload.getCommit().getId());
        assertEquals(42L, payload.getMergeRequest().getIid());

        List<Long> failedIds = payload.getFailedBuildIds();
        assertEquals(1, failedIds.size());
        assertEquals(5001L, failedIds.get(0));
    }

    @Test
    @DisplayName("Should identify non-failure pipeline")
    void shouldIdentifySuccessfulPipeline() throws Exception {
        String json = """
                {
                    "object_kind": "pipeline",
                    "object_attributes": { "id": 100, "status": "success" }
                }
                """;

        GitLabWebhookPayload payload = mapper.readValue(json, GitLabWebhookPayload.class);
        assertFalse(payload.isPipelineFailure());
    }
}

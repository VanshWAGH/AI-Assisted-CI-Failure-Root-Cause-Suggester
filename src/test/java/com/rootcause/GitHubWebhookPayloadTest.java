package com.rootcause;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rootcause.integration.github.GitHubWebhookPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHub Webhook Payload Deserialization Tests")
class GitHubWebhookPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should deserialize failed workflow_run webhook event")
    void shouldDeserializeFailedWorkflowRun() throws Exception {
        String json = """
                {
                    "action": "completed",
                    "workflow_run": {
                        "id": 88888,
                        "name": "CI Build",
                        "status": "completed",
                        "conclusion": "failure",
                        "head_branch": "feature/auth",
                        "head_sha": "f00b4r123",
                        "html_url": "https://github.com/my-org/my-repo/actions/runs/88888",
                        "pull_requests": [
                            {
                                "id": 11111,
                                "number": 42
                            }
                        ]
                    },
                    "repository": {
                        "id": 9999,
                        "name": "my-repo",
                        "full_name": "my-org/my-repo",
                        "owner": {
                            "login": "my-org"
                        }
                    }
                }
                """;

        GitHubWebhookPayload payload = mapper.readValue(json, GitHubWebhookPayload.class);

        assertEquals("completed", payload.getAction());
        assertTrue(payload.isFailedWorkflowRun());
        assertNotNull(payload.getWorkflowRun());
        assertEquals(88888L, payload.getWorkflowRun().getId());
        assertEquals("feature/auth", payload.getWorkflowRun().getHeadBranch());
        assertEquals("f00b4r123", payload.getWorkflowRun().getHeadSha());
        assertEquals("my-org/my-repo", payload.getRepository().getFullName());
        assertEquals("my-org", payload.getRepository().getOwner().getLogin());

        List<GitHubWebhookPayload.PullRequestInfo> prs = payload.getWorkflowRun().getPullRequests();
        assertEquals(1, prs.size());
        assertEquals(42, prs.get(0).getNumber());
    }

    @Test
    @DisplayName("Should identify non-failure workflow run as not target failed run")
    void shouldIdentifyNonFailureWorkflow() throws Exception {
        String json = """
                {
                    "action": "completed",
                    "workflow_run": {
                        "id": 88888,
                        "status": "completed",
                        "conclusion": "success"
                    }
                }
                """;

        GitHubWebhookPayload payload = mapper.readValue(json, GitHubWebhookPayload.class);
        assertFalse(payload.isFailedWorkflowRun());
    }

    @Test
    @DisplayName("Should identify in-progress actions as not target failed run")
    void shouldIdentifyInProgressWorkflow() throws Exception {
        String json = """
                {
                    "action": "requested",
                    "workflow_run": {
                        "id": 88888,
                        "status": "in_progress"
                    }
                }
                """;

        GitHubWebhookPayload payload = mapper.readValue(json, GitHubWebhookPayload.class);
        assertFalse(payload.isFailedWorkflowRun());
    }
}

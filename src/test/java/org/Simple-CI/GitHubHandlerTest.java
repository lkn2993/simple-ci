package org.Simple-CI;

import org.json.JSONTokener;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertTrue;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Test suite for {@link GitHubHandler#getJSON(Message)} with full node coverage
 */
public class GitHubHandlerTest {
    // Test subject
    private GitHubHandler ghh;

    @Before
    public void setUp() {
        this.ghh = new GitHubHandler();
    }

    @After
    public void tearDown() {
        this.ghh = null;
    }

    /**
     * Test case with pull request message that should generate a build
     */
    @Test
    public void testGetJSONwithValidPullRequest() {
        Message<String> msg = MessageBuilder.withPayload(
                "{\n" +
                "   action: 'opened',\n" +
                "   pull_request: {\n" +
                "       head: {\n" +
                "           ref: 'arbitraryReference',\n" +
                "           sha: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',\n" +
                "           repo: {\n" +
                "               full_name: 'Really Nice Name'\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n"
            ).build();

        Message<?> testMsg = ghh.getJSON(msg);

        String actualValue= testMsg.getHeaders().get("spawnBuild").toString();
        String expectedValue = "true";

        assertTrue(actualValue.equals(expectedValue));
    }


    /**
     * Test case with pull request message that should not generate a build
     */
    @Test
    public void testGetJSONwithInvalidPullRequest() {
        Message<String> msg = MessageBuilder.withPayload(
                "{\n" +
                        "   action: 'INVALID ACTION',\n" +
                        "   pull_request: {\n" +
                        "       head: {\n" +
                        "           ref: 'arbitraryReference',\n" +
                        "           sha: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',\n" +
                        "           repo: {\n" +
                        "               full_name: 'Really Nice Name'\n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "}\n"
        ).build();

        Message<?> testMsg = ghh.getJSON(msg);

        String actualValue= testMsg.getHeaders().get("spawnBuild").toString();
        String expectedValue = "false";

        assertTrue(actualValue.equals(expectedValue));
    }

    /**
     * Test case with pusher message that should generate a build
     */
    @Test
    public void testGetJSONwithValidPusher() {
        Message<String> msg = MessageBuilder.withPayload(
                        "{\n" +
                        "   pusher: 'test',\n" +
                        "   after: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',\n" +
                        "   repository: {\n" +
                        "       full_name: 'GOODER NAME'\n" +
                        "   },\n" +
                        "   ref: 'refs/head/branch-name'\n" +
                        "}"
        ).build();

        Message<?> testMsg = ghh.getJSON(msg);

        String actualValue= testMsg.getHeaders().get("spawnBuild").toString();
        String expectedValue = "true";

        assertTrue(actualValue.equals(expectedValue));
    }

    /**
     * Test case with pusher message that should not generate a build
     */
    @Test
    public void testGetJSONwithInvalidSha() {
        Message<String> msg = MessageBuilder.withPayload(
                "{\n" +
                        "   pusher: 'test',\n" +
                        "   after: '0000000000000000000000000000000000000000',\n" +
                        "   repository: {\n" +
                        "       full_name: 'GOODER NAME'\n" +
                        "   },\n" +
                        "   ref: 'refs/head/branch-name'\n" +
                        "}"
        ).build();

        Message<?> testMsg = ghh.getJSON(msg);

        String actualValue= testMsg.getHeaders().get("spawnBuild").toString();
        String expectedValue = "false";

        assertTrue(actualValue.equals(expectedValue));
    }
}

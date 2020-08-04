package org.Simple-CI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * This class is an HTTP client for the CI
 * The HTTP Client is responsible for notifying github about the progress made in CI in every step.
 */
@Component
public class HttpClient {
    private Logger log3 = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * Notifying Github by sending http posts of the process status, start, pending, final.
     * @param msg
     */
    @ServiceActivator(inputChannel = "gitHubNotifyChannel")
    public void notifyGithub(Message<String> msg) {
        log3.info("Inside httpclient");
        log3.info("Notification type is:" + msg.getHeaders().get("notifyType"));

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String notifyType = (String) msg.getHeaders().get("notifyType");

        if (notifyType.equals("not build")) {
            return;
        }

        BuildData build = (BuildData) msg.getHeaders().get("buildData");
        String branch = msg.getHeaders().get("branch", String.class);
        String url = "";
        String message = "";
        BuildStatus status = BuildStatus.pending;

        if (notifyType.equals("build start")) {
            message = "Build started";

        } else if (notifyType.equals("build final")) {
            url = msg.getHeaders().get("url", String.class);
            message = build.getMessage();
            status = build.getBuildStatus();
            try{
                CiApplication.CIDB.writeBuild(build);
            }catch (Exception ex){
                log3.error("Exception in HttpClient trying to write to db. " + ex.getClass().getCanonicalName());
            }

        } else if (notifyType.equals("build pending")) {
            message = "Build pending";
        }

        try {
            HttpPost request = new HttpPost("https://api.github.com/repos/" +
                    build.getRepoFullName() + "/statuses/" + build.getSha());
            request.addHeader("content-type", "application/json");

            JSONObject params = new JSONObject()
                    .put("state", status)
                    .put("target_url", url)
                    .put("description", message)
                    .put("context", "CI-DD2480");
            request.setEntity(new StringEntity(params.toString()));
            request.addHeader("Authorization", "token " + System.getenv("MY_PERSONAL_TOKEN"));
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 201){
                log3.info("Error: Unable to create http request to github. Status code: "
                        + response.getStatusLine().getStatusCode());
            }

        } catch (Exception ex) {
            log3.info("Exception in HttpClient.java httpPost. " + ex.getClass().getCanonicalName());
        }
    }
}

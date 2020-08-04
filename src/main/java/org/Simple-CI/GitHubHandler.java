package org.Simple-CI;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles the taken webhook from github in post configuration
 * This functions reads the webhook contents and notifies Spring Integration if a worker must be spawned.
 */
@Component
public class GitHubHandler {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public GitHubHandler() {

    }

    /**
     * Extracts values from github webhook event needed for build and log.
     *
     * @param msg, github webhook event json data
     * @return msg containing headers spawnbuild (true), buildData and branch if build should follow,
     * else only header spawnbuild (false).
     */
    @ServiceActivator
    public Message<?> getJSON(Message<String> msg) {
        String sha = "";
        String repoFullName = "";
        String ref = "";
        String created;
        Date date = new Date();
        BuildData buildData = null;

        // Log
        String JSONcontent = msg.getPayload();

        log.info("Inside getJSON");
        log.info("Content is:");
        log.info(JSONcontent);

        JSONObject json = new JSONObject(new JSONTokener(JSONcontent));

        boolean spawn = false;

        // pull_request event
        if(json.has("pull_request")) {
            String action = json.getString("action");
            if(action.equals("opened") || action.equals("reopened")){
                JSONObject head = json.getJSONObject("pull_request").getJSONObject("head");
                sha = head.getString("sha");
                repoFullName = head.getJSONObject("repo").getString("full_name");
                ref = head.getString("ref");
                buildData = new BuildData(repoFullName, date, sha);
                spawn = true;
            }
            //push event
        }else if (json.has("pusher")){
            sha = json.getString("after");
            if(!sha.equals("0000000000000000000000000000000000000000")){
                repoFullName = json.getJSONObject("repository").getString("full_name");
                ref = json.getString("ref");
                ref = ref.split("/", 3)[2]; // assuming refs/head/<branch-name>
                buildData = new BuildData(repoFullName, date, sha);
                spawn = true;
            }
        }

        MessageBuilder<String> newmsg = MessageBuilder.withPayload("")
                .copyHeadersIfAbsent(msg.getHeaders())
                .setHeader("http_statusCode", HttpStatus.NO_CONTENT);

        //If it should spawn build
        if(spawn){
            newmsg.setHeader("spawnBuild", "true")
                    .setHeader("buildData", buildData)
                    .setHeader("branch", ref);
        }else {
            //If it shouldn't spawn build
            newmsg.setHeader("spawnBuild", "false");
        }

        return newmsg.build();
    }
}

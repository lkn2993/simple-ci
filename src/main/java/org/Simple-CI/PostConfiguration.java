package org.Simple-CI;


import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

/**
 * This is the spring configuration class responsible for handling POST requests from github.
 * For this project we have used spring integration to implement a business flow that treats every build request sent
 * from github.
 */
@Configuration
@EnableIntegration
public class PostConfiguration {

    @Autowired
    private GitHubHandler gitHubHandler;

    /**
     *
     * A simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This channel is attached to our inbound server's reply. So, anything sends to this channel will be sent as a
     * reply to whoever has sent a request to this integration server.
     */
    @Bean
    public MessageChannel replyBridge(){
        return MessageChannels.direct("1").get();
    }

    /**
     *
     * Another simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This channel is attached to our inbound server's request. So, anything that our incoming integration server sends
     * is piped into this channel.
     */
    @Bean
    public MessageChannel requestBridge(){
        return MessageChannels.direct("2").get();
    }
    /**
     *
     * Another simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This channel simply serves to connect builder.build function to builder.replyFinal function.
     */
    @Bean
    public MessageChannel buildStatusChannel(){
        return MessageChannels.direct("3").get();
    }
    /**
     *
     * Another simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This channel is attached to whatever endpoint that is responsible for sending REST messages to github
     * (the endpoint will also authenticate with github using some token.)
     */
    @Bean
    public MessageChannel gitHubNotifyChannel(){
        return MessageChannels.direct("4").get();
    }
    /**
     *
     * Another simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This bridge is simply attached to an enriching flow that alerts notify github that we are in prebuild phase.
     */
    @Bean
    public MessageChannel preBuildBridge(){
        return MessageChannels.direct("5").get();
    }
    /**
     *
     * Another simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This channel is no longer connected to a flow and must be removed.
     * This bridge is simply attached to an enriching flow that alerts notify github that we are not in build.
     */
    @Bean
    public MessageChannel notBuildBridge(){
        return MessageChannels.direct("6").get();
    }
    /**
     *
     * Another simple direct channel which is responsible for delivering messages to the endpoint it is attached to
     * (So multiple flows can send data into the channel but only one can receive.)
     * This channel is no longer connected to a flow and must be removed.
     * This bridge is simply attached to an enriching flow that alerts notify github that we are in build phase.
     */
    @Bean
    public MessageChannel builderBridge(){
        return MessageChannels.direct("7").get();
    }
    /**
     *
     * An executor channel which is supposed to control parallel messages that are sent to it as threads.
     * This channel wraps the paralleling message in another thread wrapper, which are all synchronized. Therefore,
     * they form a blocking queue and no message can be passed into the endpoint function until the previous message
     * gets out of the system, or is wrapped in a non synchronizing thread.
     */
    @Bean
    public MessageChannel gitHubQueueBuilderChannel(){
        return MessageChannels.executor(threadQueue()).get();
    }
    /**
     *
     * A publish subscribe channel which is responsible for sending messages to all endpoints attached to it. It also
     * wraps received messages in parallel threads so their execution won't block other messages incoming from server
     * to be processed.
     */
    @Bean
    public SubscribableChannel gitHubPublishRequest()
    {
        return MessageChannels.publishSubscribe(threadExecutor())
                .get();
    }
    /**
     *
     * A task executor which is responsible for wrapping messages in parallel threads.
     */
    @Bean
    public ThreadPoolTaskExecutor threadExecutor() {

        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(102);
        return pool;
    }
    /**
     *
     * A task executor which is responsible for wrapping messages in synchronized threads.
     */
    @Bean
    public ThreadPoolTaskExecutor threadQueue() {

        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setQueueCapacity(100);
        return pool;
    }
    /**
     *
     * A router channel that is supposed to direct a message to an endpoint depending on its header contents.
     * This router channel works as follows:
     * spawnBuild = true in message header: route to preBuildBridge
     * spawnBuild = false in message header: route to notBuildBridge
     */
    @Bean
    public HeaderValueRouter routeOnSpawn() {
        HeaderValueRouter router = new HeaderValueRouter("spawnBuild");
        router.setChannelMapping("true", "preBuildBridge");
        router.setChannelMapping("false", "notBuildBridge");
        return router;
    }
    /**
     * This is the third flow which is connected to the gitHubPublishRequest channel as one of the subscribers.
     * This function is simply responsible for subscribing the routeOnSpawn router channel to
     * gitHubPublishRequest channel.
     */
    @Bean
    public IntegrationFlow gitHubBuilderChannel() {
        return IntegrationFlows.from(gitHubPublishRequest())
                .route(routeOnSpawn())
                .get();
    }
    /**
     * This is the fourth flow which is connected to the preBuildBridge channel as one of the subscribers.
     * This function is responsible for multiplying the message and send it to gitHubNotifyChannel, and after that,
     * gitHubQueueBuilderChannel. This channel is basically supposed to direct the flow to build function while
     * at the same time, invoke the notification client.
     */
    @Bean
    public IntegrationFlow preBuildFlow() {
        return IntegrationFlows.from(preBuildBridge())
                .enrichHeaders(h -> h.defaultOverwrite(true).header("notifyType", "build pending"))
                .routeToRecipients(r -> r
                        .applySequence(true)
                        .ignoreSendFailures(true)
                        .recipient(gitHubNotifyChannel())
                        .recipient(gitHubQueueBuilderChannel()))
                .get();
    }
    /**
     * This is the fifth flow which is connected to the preBuildBridge channel as one of the subscribers.
     * This function is responsible for redirecting the message to gitHubNotifyChannel, while alerting them that
     * this message is not related to build
     */
    @Bean
    public IntegrationFlow NotBuildFlow() {
        return IntegrationFlows.from(notBuildBridge())
                .enrichHeaders(h -> h.defaultOverwrite(true).header("notifyType", "not build"))
                .channel(gitHubNotifyChannel())
                .get();
    }
    /**
     * <p>This is the second flow which is connected at the other side of the requestBridge channel endpoint.
     * This function directly gets feeding by gitHubInbound flow and is responsible for the first step of request
     * processing.</p>
     * <ol>
     *     <li>Receives messages sent to requestBridge channel</li>
     *     <li>Sends the entire message to be handled by gitHubHandler class's getJSON function</li>
     *     <li>Sends the resulting message from aforementioned function's processing to gitHubPublishRequest's channel
     *     </li>
     * </ol>
     */
    @Bean
    public IntegrationFlow gitHubHandlerFlow() {
        return IntegrationFlows.from(requestBridge())
                .handle(gitHubHandler, "getJSON")
                .channel(gitHubPublishRequest())
                .get();
    }

    /**
     * This is the sixth flow which is connected to the gitHubQueueBuilderChannel channel as one of the subscribers.
     * This function is responsible for redirecting the message to gitHubNotifyChannel, while alerting them that
     * this message is not related to build, this flow entire works in serialized mode in the critical section flow
     * (After the queue)
     */
    @Bean
    public IntegrationFlow gitHubCriticalSectionFlow() {
        return IntegrationFlows.from(gitHubQueueBuilderChannel())
                .enrichHeaders(h -> h.defaultOverwrite(true).header("notifyType", "build start"))
                .routeToRecipients(r -> r
                        .applySequence(true)
                        .ignoreSendFailures(true)
                        .recipient(gitHubNotifyChannel())
                        .recipient(builderBridge()))
                .get();
    }
    /**
     * This is the seventh flow which is connected to the buildStatusChannel channel as one of the subscribers.
     * This function is responsible for redirecting the message to gitHubNotifyChannel, while alerting them that
     * this message is not related to post build,
     */
    @Bean
    public IntegrationFlow finalBuildNotifyFlow() {
        return IntegrationFlows.from(buildStatusChannel())
                .enrichHeaders(h -> h.defaultOverwrite(true).header("notifyType", "build final"))
                .channel(gitHubNotifyChannel())
                .get();
    }
    /**
     * <p>This is the first flow where it is connected to our server's incoming port on / url
     * This function is responsible for handling every request sent to the aforementioned url.
     * This functions flows every received request as follows:</p>
     * <ol>
     *     <li>Receives requests to / url</li>
     *     <li>Extracts the request's POST payload</li>
     *     <li>Checks whether the request is a JSON</li>
     *     <li>Sets reply channel to gitHubPublishRequest(so anything that is piped to this channel is sent as
     *     reply to the received request</li>
     *     <li>Sets request channel to requestBridge(so requests are pumped into this channel after they are processed
     *     </li>
     * </ol>
     */
    @Bean
    public IntegrationFlow gitHubInbound() {
        return IntegrationFlows.from(Http.inboundGateway("/")
                .requestMapping(m -> m.methods(HttpMethod.POST))
                .requestPayloadType(String.class)
                .replyChannel(gitHubPublishRequest())
                .requestChannel(requestBridge()))
                .get();
    }

}

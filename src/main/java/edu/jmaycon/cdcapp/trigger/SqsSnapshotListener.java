package edu.jmaycon.cdcapp.trigger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Builder
class SqsSnapshotListener {
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final SnapshotMessageParser messageParser;
    private final SnapshotHandler snapshotHandler;
    private final TriggerModule.Properties.SqsConfig properties;

    @Builder.Default
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    @Scheduled(fixedDelayString = "${cdcapp.trigger.sqs.poll-delay}")
    public void pollOnce() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(properties.waitTimeSeconds())
                .maxNumberOfMessages(properties.maxMessages())
                .build();
        sqsClient.receiveMessage(request).messages().forEach(this::processMessage);
    }

    private void processMessage(software.amazon.awssdk.services.sqs.model.Message message) {
        ScheduledFuture<?> heartbeat = null;
        try {
            heartbeat = startHeartbeat(message);
            snapshotHandler.handle(messageParser.parse(message.body()));
            deleteMessage(message);
        } catch (Exception e) {
            log.error("Failed to process message {}", message.messageId(), e);
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
        }
    }

    private ScheduledFuture<?> startHeartbeat(software.amazon.awssdk.services.sqs.model.Message message) {
        return heartbeatExecutor.scheduleAtFixedRate(
                () -> extendVisibility(message),
                properties.heartbeatIntervalSeconds(),
                properties.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS);
    }

    private void extendVisibility(software.amazon.awssdk.services.sqs.model.Message message) {
        try {
            sqsClient.changeMessageVisibility(b -> b.queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .visibilityTimeout(properties.visibilityExtensionSeconds()));
        } catch (Exception e) {
            log.warn("Failed to extend visibility timeout for message {}", message.messageId(), e);
        }
    }

    private void deleteMessage(software.amazon.awssdk.services.sqs.model.Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}

package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.model.SnapshotInterval;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Builder
@RequiredArgsConstructor
class SqsSnapshotListener {
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final SnapshotMessageParser messageParser;
    private final SnapshotHandler snapshotHandler;
    private final TriggerModule.Properties.SqsConfig properties;

    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void start() {
        Executors.newSingleThreadExecutor().submit(this::listen);
    }

    private void listen() {
        log.info("Starting SQS snapshot listener on queue: {}", queueUrl);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .waitTimeSeconds(properties.waitTimeSeconds())
                        .maxNumberOfMessages(properties.maxMessages())
                        .build();

                sqsClient.receiveMessage(receiveRequest).messages().forEach(this::processMessage);

                Thread.sleep(properties.pollDelay());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error receiving messages from SQS", e);
            }
        }
    }

    private void processMessage(Message message) {
        ScheduledFuture<?> heartbeat = null;
        try {
            heartbeat = startHeartbeat(message);
            SnapshotEvent event = messageParser.parse(message.body());
            snapshotHandler.handle(new SnapshotInterval(event.from(), event.to()));
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

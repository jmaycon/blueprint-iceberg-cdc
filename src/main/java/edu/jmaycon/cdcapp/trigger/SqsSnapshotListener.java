package edu.jmaycon.cdcapp.trigger;

import lombok.Builder;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Builder
public class SqsSnapshotListener {
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final SnapshotMessageParser messageParser;
    private final SnapshotHandler orchestrator;
    private final TriggerModule.Properties.SqsConfig properties;

    @Scheduled(fixedDelayString = "${cdcapp.trigger.sqs.poll-delay}")
    public void pollOnce() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(properties.waitTimeSeconds())
                .maxNumberOfMessages(properties.maxMessages())
                .build();
        sqsClient.receiveMessage(request).messages().forEach(message -> {
            orchestrator.handle(messageParser.parse(message.body()));
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        });
    }
}

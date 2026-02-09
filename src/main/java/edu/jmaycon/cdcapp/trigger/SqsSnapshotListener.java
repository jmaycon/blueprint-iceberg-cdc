package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.application.CdcOrchestrator;
import edu.jmaycon.cdcapp.config.CdcAppProperties;
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
    private final CdcOrchestrator orchestrator;
    private final CdcAppProperties.Sqs properties;

    @Scheduled(fixedDelayString = "${cdcapp.sqs.poll-delay}")
    public void pollOnce() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(properties.waitTimeSeconds())
                .maxNumberOfMessages(properties.maxMessages())
                .build();
        sqsClient.receiveMessage(request).messages().forEach(message -> {
            orchestrator.process(messageParser.parse(message.body()));
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        });
    }
}

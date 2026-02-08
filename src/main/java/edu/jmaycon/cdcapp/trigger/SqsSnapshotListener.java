package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.runtime.CdcAppProperties;
import edu.jmaycon.cdcapp.runtime.CdcOrchestrator;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public class SqsSnapshotListener {
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final SnapshotMessageParser messageParser;
    private final CdcOrchestrator orchestrator;
    private final CdcAppProperties.Sqs properties;

    public SqsSnapshotListener(
            SqsClient sqsClient,
            String queueUrl,
            SnapshotMessageParser messageParser,
            CdcOrchestrator orchestrator,
            CdcAppProperties.Sqs properties) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.messageParser = messageParser;
        this.orchestrator = orchestrator;
        this.properties = properties;
    }

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

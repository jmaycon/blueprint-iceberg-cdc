package edu.jmaycon.icerbergwriter;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public final class CdcSnapshotPublisher {
    private final SqsClient sqsClient;
    private final String queueUrl;

    public CdcSnapshotPublisher(SqsClient sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    public void publishSnapshot(long snapshotId) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageGroupId("cdc-snapshots")
                .messageBody(Long.toString(snapshotId))
                .build();
        sqsClient.sendMessage(request);
    }
}

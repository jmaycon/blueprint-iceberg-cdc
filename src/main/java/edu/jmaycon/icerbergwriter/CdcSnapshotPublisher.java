package edu.jmaycon.icerbergwriter;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@RequiredArgsConstructor
final class CdcSnapshotPublisher {
    private final SqsClient sqsClient;
    private final String queueUrl;

    public void publishSnapshot(long snapshotId) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageGroupId("cdc-snapshots")
                .messageBody(Long.toString(snapshotId))
                .build();
        sqsClient.sendMessage(request);
    }
}

package edu.jmaycon.icerbergwriter;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@RequiredArgsConstructor
final class CdcSnapshotPublisher {
    private final SqsClient sqsClient;
    private final String queueUrl;

    public void publishSnapshot(@Nullable Long fromSnapshotId, long toSnapshotId) {
        String from = fromSnapshotId == null ? "null" : Long.toString(fromSnapshotId);
        String body = String.format("{\"from\": %s, \"to\": %d}", from, toSnapshotId);
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageGroupId("cdc-snapshots")
                .messageBody(body)
                .build();
        sqsClient.sendMessage(request);
    }
}

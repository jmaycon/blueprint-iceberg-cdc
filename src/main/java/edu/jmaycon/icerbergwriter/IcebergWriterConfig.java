package edu.jmaycon.icerbergwriter;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@Configuration
public class IcebergWriterConfig {
    private static final String QUEUE_NAME = "flight_tickets.fifo";
    private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";
    private static final String AWS_REGION = "eu-central-1";

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("admin", "admin123")))
                .region(Region.of(AWS_REGION))
                .build();
    }

    @Bean
    public CdcSnapshotPublisher cdcSnapshotPublisher(SqsClient sqsClient) {
        String queueUrl = sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build())
                .queueUrl();
        return new CdcSnapshotPublisher(sqsClient, queueUrl);
    }
}

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
@org.springframework.boot.context.properties.EnableConfigurationProperties(IcebergWriterProperties.class)
public class IcebergWriterConfig {
    @Bean
    public SqsClient sqsClient(IcebergWriterProperties properties) {
        IcebergWriterProperties.Aws aws = properties.aws();
        return SqsClient.builder()
                .endpointOverride(URI.create(aws.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(aws.accessKeyId(), aws.secretAccessKey())))
                .region(Region.of(aws.region()))
                .build();
    }

    @Bean
    public CdcSnapshotPublisher cdcSnapshotPublisher(SqsClient sqsClient, IcebergWriterProperties properties) {
        String queueUrl = sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(properties.sqs().queueName())
                        .build())
                .queueUrl();
        return new CdcSnapshotPublisher(sqsClient, queueUrl);
    }
}

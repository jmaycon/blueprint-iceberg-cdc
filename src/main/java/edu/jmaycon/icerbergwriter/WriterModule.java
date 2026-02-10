package edu.jmaycon.icerbergwriter;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@Configuration
@EnableConfigurationProperties(WriterModule.Properties.class)
@RequiredArgsConstructor
class WriterModule {

    private final Properties properties;

    @Bean
    SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(properties.aws().endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.aws().accessKeyId(), properties.aws().secretAccessKey())))
                .region(Region.of(properties.aws().region()))
                .build();
    }

    @Bean
    CdcSnapshotPublisher cdcSnapshotPublisher(SqsClient sqsClient) {
        String queueUrl = sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(properties.sqs().queueName())
                        .build())
                .queueUrl();
        return new CdcSnapshotPublisher(sqsClient, queueUrl);
    }

    @ConfigurationProperties(prefix = "icebergwriter")
    public record Properties(Sqs sqs, Aws aws) {
        public record Sqs(String queueName) {}

        public record Aws(String endpoint, String region, String accessKeyId, String secretAccessKey) {}
    }
}

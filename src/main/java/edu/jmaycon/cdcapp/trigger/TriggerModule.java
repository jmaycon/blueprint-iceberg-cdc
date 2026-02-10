package edu.jmaycon.cdcapp.trigger;

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
@EnableConfigurationProperties(TriggerModule.Properties.class)
@RequiredArgsConstructor
class TriggerModule {

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
    String sqsQueueUrl(SqsClient sqsClient) {
        return sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(properties.sqs().queueName())
                        .build())
                .queueUrl();
    }

    @Bean
    SnapshotMessageParser snapshotMessageParser() {
        return new SnapshotMessageParser();
    }

    @Bean
    SqsSnapshotListener sqsSnapshotListener(
            SqsClient sqsClient,
            String sqsQueueUrl,
            SnapshotMessageParser snapshotMessageParser,
            SnapshotHandler snapshotHandler) {
        return SqsSnapshotListener.builder()
                .sqsClient(sqsClient)
                .queueUrl(sqsQueueUrl)
                .messageParser(snapshotMessageParser)
                .snapshotHandler(snapshotHandler)
                .properties(properties.sqs())
                .build();
    }

    @ConfigurationProperties(prefix = "cdcapp.trigger")
    record Properties(AwsConfig aws, SqsConfig sqs) {

        record AwsConfig(String endpoint, String region, String accessKeyId, String secretAccessKey) {}

        record SqsConfig(
                String queueName,
                long pollDelay,
                int maxMessages,
                int waitTimeSeconds,
                int heartbeatIntervalSeconds,
                int visibilityExtensionSeconds) {}
    }
}

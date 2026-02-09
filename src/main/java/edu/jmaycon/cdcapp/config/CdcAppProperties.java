package edu.jmaycon.cdcapp.config;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdcapp")
@Builder
public record CdcAppProperties(Aws aws, Sqs sqs, Kafka kafka, Iceberg iceberg, State state) {

    @Builder
    public record Aws(String endpoint, String region, String accessKeyId, String secretAccessKey) {}

    @Builder
    public record Sqs(String queueName, long pollDelay, int maxMessages, int waitTimeSeconds) {}

    public record Kafka(String bootstrapServers, String topic, String schemaRegistryUrl) {}

    @Builder
    public record Iceberg(
            String catalogUri,
            String warehouse,
            String table,
            String changelogView,
            String s3Endpoint,
            String s3Region,
            String s3AccessKey,
            String s3SecretKey) {}

    public record State(String cursorFile) {}
}

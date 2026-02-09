package edu.jmaycon.cdcapp.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdcapp")
@lombok.Builder
public record CdcAppProperties(Aws aws, Sqs sqs, Kafka kafka, Iceberg iceberg, State state) {

    @lombok.Builder
    public record Aws(String endpoint, String region, String accessKeyId, String secretAccessKey) {}

    @lombok.Builder
    public record Sqs(String queueName, long pollDelay, int maxMessages, int waitTimeSeconds) {}

    public record Kafka(String bootstrapServers, String topic) {}

    @lombok.Builder
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

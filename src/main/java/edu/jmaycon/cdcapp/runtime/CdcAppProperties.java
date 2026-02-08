package edu.jmaycon.cdcapp.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdcapp")
public record CdcAppProperties(
        Aws aws,
        Sqs sqs,
        Kafka kafka,
        Iceberg iceberg) {

    public record Aws(String endpoint, String region, String accessKeyId, String secretAccessKey) {}

    public record Sqs(String queueName, long pollDelay, int maxMessages, int waitTimeSeconds) {}

    public record Kafka(String bootstrapServers, String topic) {}

    public record Iceberg(
            String catalogUri,
            String warehouse,
            String s3Endpoint,
            String s3Region,
            String s3AccessKey,
            String s3SecretKey) {}
}

package edu.jmaycon.icerbergwriter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "icebergwriter")
public record IcebergWriterProperties(Sqs sqs, Aws aws) {
    public record Sqs(String queueName) {}

    public record Aws(String endpoint, String region, String accessKeyId, String secretAccessKey) {}
}

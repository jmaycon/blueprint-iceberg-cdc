package edu.jmaycon.downstreamconsumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "downstreamconsumer")
public record DownstreamConsumerProperties(Kafka kafka) {
    @lombok.Builder
    public record Kafka(String bootstrapServers, String topic, String groupId, boolean avroSpecificReader) {}
}

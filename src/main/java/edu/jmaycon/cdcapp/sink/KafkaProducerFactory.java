package edu.jmaycon.cdcapp.sink;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

public class KafkaProducerFactory {
    private final String bootstrapServers;

    public KafkaProducerFactory(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public KafkaProducer<byte[], byte[]> create() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("acks", "all");
        props.put("key.serializer", ByteArraySerializer.class.getName());
        props.put("value.serializer", ByteArraySerializer.class.getName());
        return new KafkaProducer<>(props);
    }
}

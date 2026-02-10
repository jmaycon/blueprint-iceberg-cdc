package edu.jmaycon.cdcapp.sink;

import edu.playground.avro.FlightTicketAvro;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@EnableConfigurationProperties(SinkModule.Properties.class)
@RequiredArgsConstructor
class SinkModule {

    private final Properties properties;

    @Bean
    ProducerFactory<String, FlightTicketAvro> kafkaProducerFactory() {
        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.bootstrapServers(),
                ProducerConfig.ACKS_CONFIG,
                "all",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class,
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                properties.schemaRegistryUrl());
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    KafkaTemplate<String, FlightTicketAvro> kafkaTemplate(ProducerFactory<String, FlightTicketAvro> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    KafkaChangePublisher kafkaChangePublisher(KafkaTemplate<String, FlightTicketAvro> kafkaTemplate) {
        return new KafkaChangePublisher(kafkaTemplate, properties.topic());
    }

    @ConfigurationProperties(prefix = "cdcapp.sink")
    record Properties(String bootstrapServers, String topic, String schemaRegistryUrl) {}
}

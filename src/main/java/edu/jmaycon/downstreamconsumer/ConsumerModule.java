package edu.jmaycon.downstreamconsumer;

import edu.playground.avro.FlightTicketAvro;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

@Configuration
@EnableConfigurationProperties(ConsumerModule.Properties.class)
@RequiredArgsConstructor
class ConsumerModule {

    private final Properties properties;

    @Bean
    ConsumerFactory<String, FlightTicketAvro> consumerFactory() {
        Properties.Kafka kafka = properties.kafka();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                kafka.groupId(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaAvroDeserializer.class,
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                kafka.schemaRegistryUrl(),
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
                kafka.avroSpecificReader());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    MessageListener<String, FlightTicketAvro> kafkaLogListener() {
        return new KafkaLogListener();
    }

    @Bean
    ConcurrentMessageListenerContainer<String, FlightTicketAvro> kafkaListenerContainer(
            ConsumerFactory<String, FlightTicketAvro> consumerFactory,
            MessageListener<String, FlightTicketAvro> kafkaLogListener) {
        ContainerProperties containerProperties =
                new ContainerProperties(properties.kafka().topic());
        containerProperties.setMessageListener(kafkaLogListener);
        return new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    @ConfigurationProperties(prefix = "downstreamconsumer")
    public record Properties(Kafka kafka) {
        public record Kafka(
                String bootstrapServers,
                String topic,
                String groupId,
                boolean avroSpecificReader,
                String schemaRegistryUrl) {}
    }
}

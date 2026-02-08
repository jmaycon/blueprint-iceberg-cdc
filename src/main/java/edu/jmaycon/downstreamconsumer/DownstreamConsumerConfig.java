package edu.jmaycon.downstreamconsumer;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

@Configuration
@EnableConfigurationProperties(DownstreamConsumerProperties.class)
public class DownstreamConsumerConfig {

    @Bean
    public ConsumerFactory<String, edu.playground.avro.FlightTicketAvro> consumerFactory(
            DownstreamConsumerProperties properties) {
        DownstreamConsumerProperties.Kafka kafka = properties.kafka();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, kafka.groupId(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, FlightTicketAvroDeserializer.class,
                "specific.avro.reader", kafka.avroSpecificReader());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public MessageListener<String, edu.playground.avro.FlightTicketAvro> kafkaLogListener() {
        return new KafkaLogListener();
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, edu.playground.avro.FlightTicketAvro>
            kafkaListenerContainer(
                    ConsumerFactory<String, edu.playground.avro.FlightTicketAvro> consumerFactory,
                    MessageListener<String, edu.playground.avro.FlightTicketAvro> kafkaLogListener,
                    DownstreamConsumerProperties properties) {
        ContainerProperties containerProperties = new ContainerProperties(properties.kafka().topic());
        containerProperties.setMessageListener(kafkaLogListener);
        return new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    }
}

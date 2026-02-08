package edu.jmaycon.cdcapp.sink;

import edu.playground.avro.FlightTicketAvro;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaChangePublisher {
    private final KafkaTemplate<String, FlightTicketAvro> kafkaTemplate;
    private final String topic;

    public KafkaChangePublisher(KafkaTemplate<String, FlightTicketAvro> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(FlightTicketAvro ticket) {
        kafkaTemplate.send(topic, ticket.getTicketUuid().toString(), ticket);
    }

    public void publishTombstone(String key) {
        kafkaTemplate.send(topic, key, null);
    }
}

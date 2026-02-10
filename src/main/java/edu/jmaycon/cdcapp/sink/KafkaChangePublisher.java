package edu.jmaycon.cdcapp.sink;

import edu.playground.avro.FlightTicketAvro;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class KafkaChangePublisher {
    private final KafkaTemplate<String, FlightTicketAvro> kafkaTemplate;
    private final String topic;

    public void publish(FlightTicketAvro ticket) {
        var unused = kafkaTemplate.send(topic, ticket.getTicketUuid().toString(), ticket);
    }

    public void publishTombstone(String key) {
        var unused = kafkaTemplate.send(topic, key, null);
    }
}

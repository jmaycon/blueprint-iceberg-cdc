package edu.jmaycon.cdcapp.sink;

import edu.playground.avro.FlightTicketAvro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class KafkaChangePublisher {
    private final KafkaTemplate<String, FlightTicketAvro> kafkaTemplate;
    private final String topic;

    public void publish(FlightTicketAvro ticket) {
        String key = ticket.getTicketUuid().toString();
        var unused = kafkaTemplate.send(topic, key, ticket).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(
                        "Sent record key={} offset={} partition={} type=UPSERT",
                        key,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send record key={} type=UPSERT", key, ex);
            }
        });
    }

    public void publishTombstone(String key) {
        var unused = kafkaTemplate.send(topic, key, null).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(
                        "Sent record key={} offset={} partition={} type=TOMBSTONE",
                        key,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send record key={} type=TOMBSTONE", key, ex);
            }
        });
    }
}

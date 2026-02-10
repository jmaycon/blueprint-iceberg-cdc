package edu.jmaycon.downstreamconsumer;

import edu.playground.avro.FlightTicketAvro;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;

@Slf4j
public class KafkaLogListener implements MessageListener<String, FlightTicketAvro> {

    @Override
    public void onMessage(ConsumerRecord<String, FlightTicketAvro> record) {
        log.info("Received ticket key={} value={}", record.key(), record.value());
    }
}

package edu.jmaycon.downstreamconsumer;

import edu.playground.avro.FlightTicketAvro;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.MessageListener;

public class KafkaLogListener implements MessageListener<String, FlightTicketAvro> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaLogListener.class);

    @Override
    public void onMessage(ConsumerRecord<String, FlightTicketAvro> record) {
        logger.info("Received ticket key={} value={}", record.key(), record.value());
    }
}

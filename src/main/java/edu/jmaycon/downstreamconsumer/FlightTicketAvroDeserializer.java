package edu.jmaycon.downstreamconsumer;

import edu.playground.avro.FlightTicketAvro;
import java.io.IOException;
import java.util.Map;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;

public class FlightTicketAvroDeserializer implements Deserializer<FlightTicketAvro> {
    private final DatumReader<FlightTicketAvro> reader = new SpecificDatumReader<>(FlightTicketAvro.class);

    @Override
    public FlightTicketAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return reader.read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize FlightTicketAvro", ex);
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public void close() {}
}

package edu.jmaycon.cdcapp.sink;

import edu.playground.avro.FlightTicketAvro;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;

public class FlightTicketAvroSerializer implements Serializer<FlightTicketAvro> {
    @Override
    public byte[] serialize(String topic, FlightTicketAvro data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            SpecificDatumWriter<FlightTicketAvro> writer = new SpecificDatumWriter<>(FlightTicketAvro.class);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(data, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize FlightTicketAvro", ex);
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public void close() {}
}

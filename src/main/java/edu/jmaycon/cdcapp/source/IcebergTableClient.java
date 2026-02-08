package edu.jmaycon.cdcapp.source;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.playground.avro.Baggage;
import edu.playground.avro.BaggageType;
import edu.playground.avro.FlightDetails;
import edu.playground.avro.FlightTicketAvro;
import edu.playground.avro.Passenger;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class IcebergTableClient {
    private final SparkSession spark;

    public IcebergTableClient(SparkSession spark) {
        this.spark = spark;
    }

    public List<FlightTicketAvro> readSnapshot(SnapshotId snapshotId) {
        Dataset<Row> dataset = spark.read()
                .format("iceberg")
                .option("snapshot-id", Long.toString(snapshotId.value()))
                .load("rest.default.flight_tickets");
        return dataset.collectAsList().stream().map(this::toFlightTicket).toList();
    }

    private FlightTicketAvro toFlightTicket(Row row) {
        Row flightRow = row.getStruct(row.fieldIndex("flight"));
        Row passengerRow = row.getStruct(row.fieldIndex("passenger"));
        List<Row> baggageRows = row.getList(row.fieldIndex("baggage"));

        FlightDetails flightDetails = FlightDetails.newBuilder()
                .setFlightNumber(flightRow.getString(0))
                .setDepartureAirport(flightRow.getString(1))
                .setArrivalAirport(flightRow.getString(2))
                .setDepartureTime(toInstant(flightRow.getTimestamp(3)))
                .setArrivalTime(toInstant(flightRow.getTimestamp(4)))
                .build();

        Passenger passenger = Passenger.newBuilder()
                .setFirstName(passengerRow.getString(0))
                .setLastName(passengerRow.getString(1))
                .setDateOfBirth(toLocalDate(passengerRow.getDate(2)))
                .build();

        List<Baggage> baggage = baggageRows.stream().map(this::toBaggage).toList();

        FlightTicketAvro.Builder builder = FlightTicketAvro.newBuilder()
                .setTicketUuid(UUID.fromString(row.getString(0)))
                .setTicketId(row.getString(1))
                .setPrice(row.getDecimal(row.fieldIndex("price")))
                .setFlight(flightDetails)
                .setPassenger(passenger)
                .setSeat(row.getString(row.fieldIndex("seat")))
                .setBaggage(baggage);

        String mealPreference = row.getString(row.fieldIndex("meal_preference"));
        if (mealPreference != null) {
            builder.setMealPreference(mealPreference);
        }

        return builder.build();
    }

    private Baggage toBaggage(Row row) {
        BigDecimal weight = row.getDecimal(0);
        String type = row.getString(1);
        return Baggage.newBuilder()
                .setWeightKg(weight)
                .setType(BaggageType.valueOf(type))
                .build();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp.toInstant();
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date.toLocalDate();
    }
}

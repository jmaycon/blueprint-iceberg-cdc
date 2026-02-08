package edu.jmaycon.icerbergwriter;

import edu.playground.avro.Baggage;
import edu.playground.avro.FlightDetails;
import edu.playground.avro.FlightTicketAvro;
import edu.playground.avro.Passenger;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IcebergWriterApplication implements CommandLineRunner {

    static {
        System.setProperty("aws.region", "eu-central-1");
        System.setProperty("aws.accessKeyId", "admin");
        System.setProperty("aws.secretAccessKey", "admin123");
    }

    public static void main(String[] args) {
        SpringApplication.run(IcebergWriterApplication.class, args);
    }

    @Override
    public void run(String... args) {
        FlightTicketAvro ticket = FlightTicketAvroDataSample.flightTicketSample();

        SparkSession spark = SparkSession.builder()
                .appName("iceberg-writer")
                .master("local[*]")
                .config("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
                .config("spark.sql.catalog.rest.uri", "http://localhost:8181")
                .config("spark.sql.catalog.rest.warehouse", "s3://analytics-warehouse")
                .config("spark.sql.catalog.rest.io-impl", "org.apache.iceberg.aws.s3.S3FileIO")
                .config("spark.sql.catalog.rest.s3.endpoint", "http://localhost:9000")
                .config("spark.sql.catalog.rest.s3.path-style-access", "true")
                .config("spark.hadoop.fs.s3a.access.key", "admin")
                .config("spark.hadoop.fs.s3a.secret.key", "admin123")
                .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.region", "eu-central-1")
                .getOrCreate();

        StructType schema = buildSchema();
        Row row = toRow(ticket);
        Dataset<Row> df = spark.createDataFrame(List.of(row), schema);

        spark.sql("CREATE NAMESPACE IF NOT EXISTS rest.default");

        df.writeTo("rest.default.flight_tickets")
                .tableProperty("format-version", "2")
                .createOrReplace();

        spark.stop();
    }

    private static StructType buildSchema() {
        StructType flightSchema = new StructType(new StructField[] {
            new StructField("flight_number", DataTypes.StringType, false, Metadata.empty()),
            new StructField("departure_airport", DataTypes.StringType, false, Metadata.empty()),
            new StructField("arrival_airport", DataTypes.StringType, false, Metadata.empty()),
            new StructField("departure_time", DataTypes.TimestampType, false, Metadata.empty()),
            new StructField("arrival_time", DataTypes.TimestampType, false, Metadata.empty())
        });

        StructType passengerSchema = new StructType(new StructField[] {
            new StructField("first_name", DataTypes.StringType, false, Metadata.empty()),
            new StructField("last_name", DataTypes.StringType, false, Metadata.empty()),
            new StructField("date_of_birth", DataTypes.DateType, false, Metadata.empty())
        });

        StructType baggageSchema = new StructType(new StructField[] {
            new StructField("weight_kg", DataTypes.createDecimalType(5, 3), false, Metadata.empty()),
            new StructField("type", DataTypes.StringType, false, Metadata.empty())
        });

        return new StructType(new StructField[] {
            new StructField("ticket_uuid", DataTypes.StringType, false, Metadata.empty()),
            new StructField("ticket_id", DataTypes.StringType, false, Metadata.empty()),
            new StructField("price", DataTypes.createDecimalType(10, 2), false, Metadata.empty()),
            new StructField("flight", flightSchema, false, Metadata.empty()),
            new StructField("passenger", passengerSchema, false, Metadata.empty()),
            new StructField("seat", DataTypes.StringType, false, Metadata.empty()),
            new StructField("baggage", DataTypes.createArrayType(baggageSchema, false), false, Metadata.empty()),
            new StructField("meal_preference", DataTypes.StringType, true, Metadata.empty())
        });
    }

    private static Row toRow(FlightTicketAvro ticket) {
        FlightDetails flight = ticket.getFlight();
        Passenger passenger = ticket.getPassenger();

        Row flightRow = RowFactory.create(
                flight.getFlightNumber(),
                flight.getDepartureAirport(),
                flight.getArrivalAirport(),
                toTimestamp(flight.getDepartureTime()),
                toTimestamp(flight.getArrivalTime()));

        Row passengerRow = RowFactory.create(
                passenger.getFirstName(), passenger.getLastName(), Date.valueOf(passenger.getDateOfBirth()));

        List<Row> baggageRows = ticket.getBaggage().stream()
                .map(IcebergWriterApplication::toBaggageRow)
                .toList();

        return RowFactory.create(
                ticket.getTicketUuid().toString(),
                ticket.getTicketId(),
                ticket.getPrice(),
                flightRow,
                passengerRow,
                ticket.getSeat(),
                baggageRows,
                ticket.getMealPreference());
    }

    private static Row toBaggageRow(Baggage baggage) {
        BigDecimal weight = baggage.getWeightKg();
        return RowFactory.create(weight, baggage.getType().name());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}

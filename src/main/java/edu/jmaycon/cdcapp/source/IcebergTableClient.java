package edu.jmaycon.cdcapp.source;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@RequiredArgsConstructor
public class IcebergTableClient {
    private final SparkSession spark;
    private final FlightTicketRowMapper rowMapper;

    public List<FlightTicketAvro> readSnapshot(SnapshotId snapshotId, String table) {
        Dataset<Row> dataset = spark.read()
                .format("iceberg")
                .option("snapshot-id", Long.toString(snapshotId.value()))
                .load(table);
        return dataset.collectAsList().stream().map(rowMapper::map).toList();
    }
}

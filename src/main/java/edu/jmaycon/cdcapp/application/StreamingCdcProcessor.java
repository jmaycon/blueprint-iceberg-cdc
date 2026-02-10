package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.model.SnapshotInterval;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
@RequiredArgsConstructor
@Builder
class StreamingCdcProcessor implements CdcChangeProcessor, AutoCloseable {
    private static final String QUERY_NAME_PREFIX = "cdc_streaming_processor_";

    private final SparkSession sparkSession;
    private final FlightTicketRowMapper rowMapper;
    private final KafkaChangePublisher changePublisher;
    private final String table;

    @Override
    public void process(SnapshotInterval interval) {
        if (isQueryRunning()) {
            log.info("Streaming query for table {} is already running", table);
            return;
        }

        SnapshotId from = interval.from();
        SnapshotId to = interval.to();
        log.info("Starting streaming CDC processor for snapshot trigger: from={} to={}", from, to);

        Dataset<Row> stream = sparkSession.readStream().format("iceberg").load(table);

        try {
            stream.writeStream()
                    .queryName(queryName())
                    .foreachBatch((batch, batchId) -> {
                        log.debug("Processing streaming batch {}", batchId);
                        batch.collectAsList().forEach(this::forwardChange);
                    })
                    .start();
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Timed out while starting streaming query for table: " + table, ex);
        }
    }

    @Override
    public void close() {
        Arrays.stream(sparkSession.streams().active())
                .filter(q -> q.name().equals(queryName()))
                .forEach(query -> {
                    try {
                        query.stop();
                    } catch (TimeoutException e) {
                        log.warn("Timed out while stopping streaming query: {}", query.name(), e);
                    }
                });
    }

    private boolean isQueryRunning() {
        return Arrays.stream(sparkSession.streams().active())
                .anyMatch(q -> q.name().equals(queryName()));
    }

    private String queryName() {
        return QUERY_NAME_PREFIX + table.replace('.', '_');
    }

    private void forwardChange(Row row) {
        // Ignore UPDATE_BEFORE events as they are immediately followed by an
        // UPDATE_AFTER.
        // Skipping them avoids redundant tombstone-upsert pairs in the downstream sink.
        if ("UPDATE_BEFORE".equals(row.getString(row.fieldIndex("_change_type")))) {
            return;
        }

        String changeType = row.getString(row.fieldIndex("_change_type"));
        String ticketId = row.getString(row.fieldIndex("ticket_uuid"));

        if ("DELETE".equals(changeType)) {
            changePublisher.publishTombstone(ticketId);
            return;
        }

        changePublisher.publish(rowMapper.map(row));
    }
}

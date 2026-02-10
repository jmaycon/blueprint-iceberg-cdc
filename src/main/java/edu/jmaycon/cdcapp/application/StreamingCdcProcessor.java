package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;

@Slf4j
@RequiredArgsConstructor
@Builder
class StreamingCdcProcessor implements CdcChangeProcessor, AutoCloseable {
    private final SparkSession sparkSession;
    private final FlightTicketRowMapper rowMapper;
    private final KafkaChangePublisher changePublisher;
    private final String table;

    @Builder.Default
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Builder.Default
    private final AtomicReference<StreamingQuery> streamingQuery = new AtomicReference<>();

    @Override
    public void process(SnapshotId snapshotId) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Streaming query already started");
        }
        log.info("Starting streaming CDC processor for snapshot trigger: {}", snapshotId);

        Dataset<Row> stream = sparkSession.readStream().format("iceberg").load(table);

        try {
            streamingQuery.set(stream.writeStream()
                    .foreachBatch((batch, batchId) -> {
                        log.debug("Processing streaming batch {}", batchId);
                        batch.collectAsList().forEach(this::publishChange);
                    })
                    .start());
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Timed out while starting streaming query", ex);
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            StreamingQuery query = streamingQuery.get();
            if (query != null) {
                try {
                    query.stop();
                } catch (TimeoutException e) {
                    log.warn("Timed out while stopping streaming query", e);
                }
            }
        }
    }

    private void publishChange(Row row) {
        String changeType = row.getString(row.fieldIndex("_change_type"));
        String ticketId = row.getString(row.fieldIndex("ticket_uuid"));
        if ("DELETE".equals(changeType) || "UPDATE_BEFORE".equals(changeType)) {
            changePublisher.publishTombstone(ticketId);
            return;
        }
        changePublisher.publish(rowMapper.map(row));
    }
}

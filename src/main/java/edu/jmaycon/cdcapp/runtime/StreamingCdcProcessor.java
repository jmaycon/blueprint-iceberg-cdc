package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class StreamingCdcProcessor implements CdcChangeProcessor {
    private final SparkSession sparkSession;
    private final FlightTicketRowMapper rowMapper;
    private final KafkaChangePublisher changePublisher;
    private final CdcAppProperties.Iceberg iceberg;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void process(SnapshotId snapshotId) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Streaming query already started");
        }

        Dataset<Row> stream = sparkSession.readStream().format("iceberg").load(iceberg.table());

        try {
            stream.writeStream()
                    .foreachBatch((batch, batchId) -> {
                        batch.collectAsList().forEach(row -> changePublisher.publish(rowMapper.map(row)));
                    })
                    .start();
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Timed out while starting streaming query", ex);
        }
    }
}

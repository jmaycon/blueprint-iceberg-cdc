package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import edu.jmaycon.cdcapp.state.CursorStore;
import edu.playground.avro.FlightTicketAvro;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Builder
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChangelogCdcProcessor implements CdcChangeProcessor {
    private final SparkSession sparkSession;
    private final FlightTicketRowMapper rowMapper;
    private final KafkaChangePublisher changePublisher;
    private final CursorStore cursorStore;
    private final CdcAppProperties.Iceberg iceberg;

    @Override
    public void process(SnapshotId snapshotId) {
        Optional<SnapshotId> previousSnapshot = cursorStore.load();
        if (previousSnapshot.isEmpty()) {
            publishSnapshotSnapshot(snapshotId);
            cursorStore.save(snapshotId);
            return;
        }

        SnapshotId startSnapshot = previousSnapshot.get();
        try {
            createChangelogView(startSnapshot, snapshotId);
            Dataset<Row> changes = sparkSession.table(tempChangelogViewName());
            changes.collectAsList().forEach(this::publishChange);
            cursorStore.save(snapshotId);
        } catch (IllegalArgumentException ex) {
            // Fallback to a full snapshot when the stored cursor isn't an ancestor.
            publishSnapshotSnapshot(snapshotId);
            cursorStore.save(snapshotId);
        }
    }

    private void publishSnapshotSnapshot(SnapshotId snapshotId) {
        Dataset<Row> snapshot = sparkSession
                .read()
                .format("iceberg")
                .option("snapshot-id", Long.toString(snapshotId.value()))
                .load(iceberg.table());
        snapshot.collectAsList().forEach(row -> changePublisher.publish(rowMapper.map(row)));
    }

    private void createChangelogView(SnapshotId startSnapshot, SnapshotId endSnapshot) {
        String tempViewName = tempChangelogViewName();
        String statement = """
                CALL rest.system.create_changelog_view(
                  '%s',
                  '%s',
                  map('start-snapshot-id','%d','end-snapshot-id','%d'))
                """.formatted(iceberg.table(), tempViewName, startSnapshot.value(), endSnapshot.value());
        sparkSession.sql(statement);
    }

    private String tempChangelogViewName() {
        String configured = iceberg.changelogView();
        int lastDot = configured.lastIndexOf('.');
        if (lastDot == -1 || lastDot == configured.length() - 1) {
            return configured;
        }
        return configured.substring(lastDot + 1);
    }

    private void publishChange(Row row) {
        String changeType = row.getString(row.fieldIndex("_change_type"));
        String ticketId = row.getString(row.fieldIndex("ticket_uuid"));
        if ("DELETE".equals(changeType) || "UPDATE_BEFORE".equals(changeType)) {
            changePublisher.publishTombstone(ticketId);
            return;
        }
        FlightTicketAvro ticket = rowMapper.map(row);
        changePublisher.publish(ticket);
    }
}

package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.model.SnapshotInterval;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import edu.jmaycon.cdcapp.state.CursorStore;
import edu.playground.avro.FlightTicketAvro;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
@Builder
@RequiredArgsConstructor
class ChangelogCdcProcessor implements CdcChangeProcessor {
    private final SparkSession sparkSession;
    private final FlightTicketRowMapper rowMapper;
    private final KafkaChangePublisher changePublisher;
    private final CursorStore cursorStore;
    private final String table;
    private final String changelogView;

    @Override
    public void process(SnapshotInterval interval) {
        SnapshotId from = interval.from();
        SnapshotId to = interval.to();
        if (from != null) {
            try {
                processIncrementalChanges(from, to);
                cursorStore.save(to);
            } catch (IllegalArgumentException ex) {
                log.warn(
                        "Stored snapshot cursor {} is not an ancestor of current snapshot {}. Falling back to full snapshot.",
                        from,
                        to,
                        ex);
                processFullSnapshot(to);
                cursorStore.save(to);
            }
        } else {
            processFullSnapshot(to);
            cursorStore.save(to);
        }
    }

    private void processIncrementalChanges(SnapshotId from, SnapshotId to) {
        createChangelogView(from, to);
        Dataset<Row> changes = sparkSession.table(tempChangelogViewName());
        changes.collectAsList().forEach(this::forwardChange);
    }

    private void processFullSnapshot(SnapshotId snapshotId) {
        Dataset<Row> snapshot = sparkSession
                .read()
                .format("iceberg")
                .option("snapshot-id", Long.toString(snapshotId.value()))
                .load(table);
        snapshot.collectAsList().forEach(row -> changePublisher.publish(rowMapper.map(row)));
    }

    private void createChangelogView(SnapshotId startSnapshot, SnapshotId endSnapshot) {
        String tempViewName = tempChangelogViewName();
        String statement = """
                CALL rest.system.create_changelog_view(
                  '%s',
                  '%s',
                  map('start-snapshot-id','%d','end-snapshot-id','%d'))
                """.formatted(table, tempViewName, startSnapshot.value(), endSnapshot.value());
        sparkSession.sql(statement);
    }

    private String tempChangelogViewName() {
        String configured = changelogView;
        int lastDot = configured.lastIndexOf('.');
        if (lastDot == -1 || lastDot == configured.length() - 1) {
            return configured;
        }
        return configured.substring(lastDot + 1);
    }

    private void forwardChange(Row row) {
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

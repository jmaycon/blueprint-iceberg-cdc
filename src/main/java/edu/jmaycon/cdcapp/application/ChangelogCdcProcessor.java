package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.model.SnapshotInterval;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.FlightTicketRowMapper;
import edu.jmaycon.cdcapp.state.CursorStore;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.support.SendResult;

@Slf4j
@Builder
@RequiredArgsConstructor
class ChangelogCdcProcessor {
    private final SparkSession sparkSession;
    private final FlightTicketRowMapper rowMapper;
    private final KafkaChangePublisher changePublisher;
    private final CursorStore cursorStore;
    private final String table;
    private final String changelogView;

    public void process(SnapshotInterval interval) {
        SnapshotId to = Objects.requireNonNull(interval.to());
        SnapshotId from = interval.from();

        log.info("Processing CDC interval: from={} to={}", from, to);

        if (from == null) {
            waitFor(processFullSnapshot(to));
            cursorStore.save(to);
            return;
        }

        try {
            waitFor(processIncrementalChanges(from, to));
            cursorStore.save(to);
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "Stored snapshot cursor {} is not an ancestor of current snapshot {}. Falling back to full snapshot.",
                    from,
                    to,
                    ex);
            waitFor(processFullSnapshot(to));
            cursorStore.save(to);
        }
    }

    private void waitFor(List<CompletableFuture<SendResult<String, FlightTicketAvro>>> futures) {
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private List<CompletableFuture<SendResult<String, FlightTicketAvro>>> processIncrementalChanges(
            SnapshotId from, SnapshotId to) {
        createChangelogView(from, to);
        return sparkSession.table(tempChangelogViewName()).collectAsList().stream()
                .map(this::forwardChange)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<CompletableFuture<SendResult<String, FlightTicketAvro>>> processFullSnapshot(SnapshotId snapshotId) {
        Dataset<Row> snapshot = sparkSession
                .read()
                .format("iceberg")
                .option("snapshot-id", Long.toString(snapshotId.value()))
                .load(table);
        return snapshot.collectAsList().stream()
                .map(row -> changePublisher.publish(rowMapper.map(row)))
                .toList();
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

    @Nullable
    private CompletableFuture<SendResult<String, FlightTicketAvro>> forwardChange(Row row) {
        // Ignore UPDATE_BEFORE events as they are immediately followed by an
        // UPDATE_AFTER.
        // Skipping them avoids redundant tombstone-upsert pairs in the downstream sink.
        if ("UPDATE_BEFORE".equals(row.getString(row.fieldIndex("_change_type")))) {
            return null;
        }

        String changeType = row.getString(row.fieldIndex("_change_type"));
        String ticketId = row.getString(row.fieldIndex("ticket_uuid"));

        if ("DELETE".equals(changeType)) {
            return changePublisher.publishTombstone(ticketId);
        }

        return changePublisher.publish(rowMapper.map(row));
    }
}

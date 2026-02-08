package edu.jmaycon.cdcapp.source;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;

public class IcebergSnapshotReader {
    private final SnapshotPlanner snapshotPlanner;
    private final IcebergChangelogReader changelogReader;

    public IcebergSnapshotReader(SnapshotPlanner snapshotPlanner, IcebergChangelogReader changelogReader) {
        this.snapshotPlanner = snapshotPlanner;
        this.changelogReader = changelogReader;
    }

    public List<FlightTicketAvro> readSnapshot(SnapshotId snapshotId) {
        SnapshotId plannedSnapshot = snapshotPlanner.plan(snapshotId);
        return changelogReader.readSnapshot(plannedSnapshot);
    }
}

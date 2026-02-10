package edu.jmaycon.cdcapp.source;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IcebergSnapshotReader {
    private final IcebergChangelogReader changelogReader;

    public List<FlightTicketAvro> readSnapshot(SnapshotId snapshotId) {
        return changelogReader.readSnapshot(snapshotId);
    }
}

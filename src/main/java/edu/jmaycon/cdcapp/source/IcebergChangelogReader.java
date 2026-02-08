package edu.jmaycon.cdcapp.source;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;

public class IcebergChangelogReader {
    private final IcebergTableClient tableClient;

    public IcebergChangelogReader(IcebergTableClient tableClient) {
        this.tableClient = tableClient;
    }

    public List<FlightTicketAvro> readSnapshot(SnapshotId snapshotId) {
        return tableClient.readSnapshot(snapshotId);
    }
}

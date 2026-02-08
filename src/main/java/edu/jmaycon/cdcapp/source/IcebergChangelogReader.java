package edu.jmaycon.cdcapp.source;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;

public class IcebergChangelogReader {
    private final IcebergTableClient tableClient;
    private final String table;

    public IcebergChangelogReader(IcebergTableClient tableClient, String table) {
        this.tableClient = tableClient;
        this.table = table;
    }

    public List<FlightTicketAvro> readSnapshot(SnapshotId snapshotId) {
        return tableClient.readSnapshot(snapshotId, table);
    }
}

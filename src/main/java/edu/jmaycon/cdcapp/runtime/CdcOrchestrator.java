package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.sink.KafkaChangePublisher;
import edu.jmaycon.cdcapp.source.IcebergSnapshotReader;
import edu.playground.avro.FlightTicketAvro;
import java.util.List;

public class CdcOrchestrator {
    private final IcebergSnapshotReader snapshotReader;
    private final KafkaChangePublisher changePublisher;

    public CdcOrchestrator(IcebergSnapshotReader snapshotReader, KafkaChangePublisher changePublisher) {
        this.snapshotReader = snapshotReader;
        this.changePublisher = changePublisher;
    }

    public void process(SnapshotId snapshotId) {
        List<FlightTicketAvro> tickets = snapshotReader.readSnapshot(snapshotId);
        tickets.forEach(changePublisher::publish);
    }
}

package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.model.SnapshotInterval;

public interface SnapshotHandler {
    void handle(SnapshotInterval interval);
}

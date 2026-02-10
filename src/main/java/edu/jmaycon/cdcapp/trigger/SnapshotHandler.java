package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.model.SnapshotId;

public interface SnapshotHandler {
    void handle(SnapshotId from, SnapshotId to);
}

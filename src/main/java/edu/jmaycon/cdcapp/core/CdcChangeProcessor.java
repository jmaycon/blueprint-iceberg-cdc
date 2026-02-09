package edu.jmaycon.cdcapp.core;

import edu.jmaycon.cdcapp.model.SnapshotId;

public interface CdcChangeProcessor {
    void process(SnapshotId snapshotId);
}

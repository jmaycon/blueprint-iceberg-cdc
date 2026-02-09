package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;

public interface CdcChangeProcessor {
    void process(SnapshotId snapshotId);
}

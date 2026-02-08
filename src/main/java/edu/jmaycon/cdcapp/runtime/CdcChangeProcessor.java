package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.model.SnapshotId;

public interface CdcChangeProcessor {
    void process(SnapshotId snapshotId);
}

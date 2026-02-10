package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;

interface CdcChangeProcessor {
    void process(SnapshotId from, SnapshotId to);
}

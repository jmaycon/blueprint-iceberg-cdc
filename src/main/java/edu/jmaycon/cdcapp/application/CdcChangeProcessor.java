package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotInterval;

public interface CdcChangeProcessor {
    void process(SnapshotInterval interval);
}

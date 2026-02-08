package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.model.SnapshotId;

public class CdcOrchestrator {
    private final CdcChangeProcessor changeProcessor;

    public CdcOrchestrator(CdcChangeProcessor changeProcessor) {
        this.changeProcessor = changeProcessor;
    }

    public void process(SnapshotId snapshotId) {
        changeProcessor.process(snapshotId);
    }
}

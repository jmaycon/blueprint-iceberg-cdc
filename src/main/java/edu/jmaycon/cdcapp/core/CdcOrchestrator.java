package edu.jmaycon.cdcapp.core;

import edu.jmaycon.cdcapp.model.SnapshotId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CdcOrchestrator {
    private final CdcChangeProcessor changeProcessor;

    public void process(SnapshotId snapshotId) {
        changeProcessor.process(snapshotId);
    }
}

package edu.jmaycon.cdcapp.runtime;

import edu.jmaycon.cdcapp.model.SnapshotId;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CdcOrchestrator {
    private final CdcChangeProcessor changeProcessor;

    public void process(SnapshotId snapshotId) {
        changeProcessor.process(snapshotId);
    }
}

package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotId;
import edu.jmaycon.cdcapp.trigger.SnapshotHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CdcOrchestrator implements SnapshotHandler {
    private final CdcChangeProcessor changeProcessor;

    @Override
    public void handle(SnapshotId snapshotId) {
        changeProcessor.process(snapshotId);
    }
}

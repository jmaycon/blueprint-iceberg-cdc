package edu.jmaycon.cdcapp.application;

import edu.jmaycon.cdcapp.model.SnapshotInterval;
import edu.jmaycon.cdcapp.trigger.SnapshotHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CdcOrchestrator implements SnapshotHandler {
    private final CdcChangeProcessor changeProcessor;

    @Override
    public void handle(SnapshotInterval interval) {
        changeProcessor.process(interval);
    }
}

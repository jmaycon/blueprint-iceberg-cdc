package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.model.SnapshotId;

public class SnapshotMessageParser {
    public SnapshotId parse(String body) {
        return new SnapshotId(Long.parseLong(body.trim()));
    }
}

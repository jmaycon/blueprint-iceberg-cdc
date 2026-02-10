package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.model.SnapshotId;
import org.jspecify.annotations.Nullable;

record SnapshotEvent(@Nullable SnapshotId from, SnapshotId to) {}

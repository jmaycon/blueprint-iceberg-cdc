package edu.jmaycon.cdcapp.trigger;

import edu.jmaycon.cdcapp.model.SnapshotId;
import jakarta.annotation.Nullable;

record SnapshotEvent(@Nullable SnapshotId from, SnapshotId to) {}

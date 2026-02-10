package edu.jmaycon.cdcapp.model;

import org.jspecify.annotations.Nullable;

public record SnapshotInterval(@Nullable SnapshotId from, SnapshotId to) {}
